package com.mike.dynamicisland.faceid

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FaceUnlockPhase(val label: String) {
    IDLE("En veille"),
    SCANNING("Analyse / Écran verrouillé"),
    SUCCESS("Déverrouillé"),
    INTERRUPTED("Interrompu")
}

/**
 * Distingue explicitement les événements système réellement captés des modes dégradés ou tests.
 * MIKE ne prétend jamais avoir reçu une preuve biométrique faciale quand l'OS ne l'expose pas.
 */
enum class DetectionProvenance(val badge: String, val isRealSystemEvent: Boolean) {
    NONE("Inactif", false),
    SYSTEM_SCREEN_ON_LOCKED("RÉEL · SCREEN_ON + Keyguard", true),
    SYSTEM_USER_PRESENT_DEGRADED("DÉGRADÉ · USER_PRESENT (Keyguard)", true),
    SYSTEM_SCREEN_OFF_INTERRUPTED("RÉEL · SCREEN_OFF (Interruption)", true),
    MANUAL_TEST("TEST · Déclenchement manuel", false)
}

data class FaceBiometricCapabilityReport(
    val hasSystemFaceFeature: Boolean,
    val hasVendorDirectFaceApi: Boolean,
    val isHiOsOrTecno: Boolean,
    val operatingModeLabel: String,
    val honestExplanation: String
)

data class FaceUnlockState(
    val phase: FaceUnlockPhase = FaceUnlockPhase.IDLE,
    val isScreenOn: Boolean = true,
    val isKeyguardLocked: Boolean = false,
    val provenance: DetectionProvenance = DetectionProvenance.NONE,
    val lastEventTimestampMs: Long = 0L,
    val unlockElapsedMs: Long? = null,
    val sequenceId: Long = 0L,
    val capability: FaceBiometricCapabilityReport = FaceBiometricCapabilityReport(
        hasSystemFaceFeature = false,
        hasVendorDirectFaceApi = false,
        isHiOsOrTecno = false,
        operatingModeLabel = "Mode Dégradé — Événements Keyguard",
        honestExplanation = "ACTION_USER_PRESENT confirme uniquement que l'appareil est déverrouillé (visage, PIN ou schéma)."
    )
)

/**
 * Moteur de détection du cycle verrouillage/déverrouillage (Module 01).
 * Suit ACTION_SCREEN_ON, ACTION_SCREEN_OFF, ACTION_USER_PRESENT et KeyguardManager
 * avec transparence totale sur les limites d'Android 10 / HiOS 6.0 sans root.
 */
class FaceUnlockEngine(
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() },
    private val onEventLogged: (category: String, message: String, isRealEvent: Boolean) -> Unit = { _, _, _ -> }
) {
    private val _state = MutableStateFlow(FaceUnlockState())
    val state: StateFlow<FaceUnlockState> = _state.asStateFlow()

    private var screenOnAtMs: Long = 0L

    fun inspectCapabilities(context: Context) {
        val pm = context.packageManager
        val hasFaceFeature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { pm.hasSystemFeature(PackageManager.FEATURE_FACE) }.getOrDefault(false)
        } else {
            false
        }
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val model = Build.MODEL.orEmpty()
        val isTecno = manufacturer.contains("TECNO", ignoreCase = true) ||
            brand.contains("TECNO", ignoreCase = true) ||
            model.contains("CD6", ignoreCase = true)

        // Aucune API publique sans root n'est exposée par HiOS 6.0 / Android 10 pour distinguer
        // Face Unlock du PIN/schéma : on l'indique explicitement.
        val hasVendorApi = false

        val report = FaceBiometricCapabilityReport(
            hasSystemFaceFeature = hasFaceFeature,
            hasVendorDirectFaceApi = hasVendorApi,
            isHiOsOrTecno = isTecno,
            operatingModeLabel = if (isTecno) {
                "Mode Dégradé HiOS 6.0 (Keyguard + SCREEN_ON / USER_PRESENT)"
            } else {
                "Mode Dégradé Android (Keyguard + SCREEN_ON / USER_PRESENT)"
            },
            honestExplanation = if (hasFaceFeature) {
                "Capteur facial système présent, mais Android ne divulgue pas aux apps tierces si le déverrouillage provient du visage ou du PIN. Synchronisation réelle sur SCREEN_ON et USER_PRESENT."
            } else {
                "Aucune API biométrique faciale publique détectée (fréquent sur Android 10 / HiOS 6.0). Synchronisation fiable en mode dégradé sur KeyguardManager + ACTION_USER_PRESENT."
            }
        )

        val keyguardLocked = isKeyguardCurrentlyLocked(context)
        _state.update {
            it.copy(
                capability = report,
                isKeyguardLocked = keyguardLocked
            )
        }
        onEventLogged("FaceID", "Capacités inspectées : ${report.operatingModeLabel}", true)
    }

    fun isKeyguardCurrentlyLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    /**
     * Appelé lors de la réception réelle d'ACTION_SCREEN_ON.
     */
    fun onScreenOn(keyguardLocked: Boolean) {
        val now = timeProvider()
        screenOnAtMs = now
        if (keyguardLocked) {
            _state.update {
                it.copy(
                    phase = FaceUnlockPhase.SCANNING,
                    isScreenOn = true,
                    isKeyguardLocked = true,
                    provenance = DetectionProvenance.SYSTEM_SCREEN_ON_LOCKED,
                    lastEventTimestampMs = now,
                    unlockElapsedMs = null,
                    sequenceId = it.sequenceId + 1
                )
            }
            onEventLogged("FaceID", "Écran allumé sur écran verrouillé (Keyguard actif) — démarrage scan", true)
        } else {
            _state.update {
                it.copy(
                    phase = FaceUnlockPhase.IDLE,
                    isScreenOn = true,
                    isKeyguardLocked = false,
                    lastEventTimestampMs = now
                )
            }
            onEventLogged("FaceID", "Écran allumé sans verrouillage actif — animation en veille", true)
        }
    }

    /**
     * Appelé lors de la réception réelle d'ACTION_SCREEN_OFF.
     * Coupe immédiatement toute animation pour préserver batterie et GPU.
     */
    fun onScreenOff() {
        val now = timeProvider()
        val wasScanning = _state.value.phase == FaceUnlockPhase.SCANNING
        _state.update {
            it.copy(
                phase = if (wasScanning) FaceUnlockPhase.INTERRUPTED else FaceUnlockPhase.IDLE,
                isScreenOn = false,
                isKeyguardLocked = true,
                provenance = if (wasScanning) {
                    DetectionProvenance.SYSTEM_SCREEN_OFF_INTERRUPTED
                } else {
                    DetectionProvenance.NONE
                },
                lastEventTimestampMs = now,
                sequenceId = it.sequenceId + 1
            )
        }
        val msg = if (wasScanning) {
            "Écran éteint pendant l'analyse — animation interrompue et suspendue"
        } else {
            "Écran éteint — toutes les animations Face ID suspendues"
        }
        onEventLogged("FaceID", msg, true)
    }

    /**
     * Appelé lors de la réception réelle d'ACTION_USER_PRESENT.
     * Note d'intégrité : prouve le déverrouillage système, jamais exclusivement la biométrie faciale.
     */
    fun onUserPresent(allowDegradedUnlockAnimation: Boolean = true) {
        val now = timeProvider()
        val elapsed = if (screenOnAtMs > 0L) (now - screenOnAtMs).coerceAtLeast(0L) else null
        if (!allowDegradedUnlockAnimation) {
            _state.update {
                it.copy(
                    phase = FaceUnlockPhase.IDLE,
                    isScreenOn = true,
                    isKeyguardLocked = false,
                    lastEventTimestampMs = now,
                    unlockElapsedMs = elapsed
                )
            }
            onEventLogged("FaceID", "USER_PRESENT reçu mais mode dégradé désactivé par l'utilisateur", true)
            return
        }
        _state.update {
            it.copy(
                phase = FaceUnlockPhase.SUCCESS,
                isScreenOn = true,
                isKeyguardLocked = false,
                provenance = DetectionProvenance.SYSTEM_USER_PRESENT_DEGRADED,
                lastEventTimestampMs = now,
                unlockElapsedMs = elapsed,
                sequenceId = it.sequenceId + 1
            )
        }
        onEventLogged(
            "FaceID",
            "USER_PRESENT détecté (${elapsed ?: 0} ms) — validation visuelle en mode dégradé Keyguard",
            true
        )
    }

    /**
     * Déclenche une séquence de test contrôlée depuis l'interface de personnalisation ou diagnostic.
     */
    fun triggerManualTest(simulateInterrupt: Boolean = false) {
        val now = timeProvider()
        val nextPhase = if (simulateInterrupt) FaceUnlockPhase.INTERRUPTED else FaceUnlockPhase.SCANNING
        _state.update {
            it.copy(
                phase = nextPhase,
                isScreenOn = true,
                provenance = DetectionProvenance.MANUAL_TEST,
                lastEventTimestampMs = now,
                unlockElapsedMs = null,
                sequenceId = it.sequenceId + 1
            )
        }
        onEventLogged(
            "FaceID",
            if (simulateInterrupt) "Test manuel : interruption du scan" else "Test manuel : séquence Face ID complète",
            false
        )
    }

    fun completeManualTestToSuccess() {
        val now = timeProvider()
        if (_state.value.provenance != DetectionProvenance.MANUAL_TEST) return
        _state.update {
            it.copy(
                phase = FaceUnlockPhase.SUCCESS,
                lastEventTimestampMs = now,
                sequenceId = it.sequenceId + 1
            )
        }
    }

    fun markDismissed() {
        _state.update {
            it.copy(phase = FaceUnlockPhase.IDLE)
        }
    }
}
