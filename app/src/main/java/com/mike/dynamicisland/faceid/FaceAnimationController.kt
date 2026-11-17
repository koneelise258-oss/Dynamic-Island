package com.mike.dynamicisland.faceid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Contrôleur d'animation Face ID basé sur Animatable (Module 01).
 * Suspend immédiatement tout travail graphique lorsque l'écran est éteint ou en veille.
 */
@Stable
class FaceAnimationController {
    val progress = Animatable(0f)
    var isVisible by mutableStateOf(false)
        private set
    var isSuccess by mutableStateOf(false)
        private set
    var isInterrupted by mutableStateOf(false)
        private set

    suspend fun runForState(
        unlockState: FaceUnlockState,
        settings: FaceAnimationSettings,
        onCompleted: () -> Unit = {},
        onSuccessReached: () -> Unit = {}
    ) {
        if (!unlockState.isScreenOn || !settings.enabled) {
            progress.snapTo(0f)
            isVisible = false
            isSuccess = false
            isInterrupted = false
            return
        }

        val speed = settings.speedMultiplier.coerceIn(0.5f, 2.2f)
        val scanDuration = (settings.durationMs / speed).toInt().coerceIn(350, 2600)
        val holdMs = settings.holdAfterSuccessMs.coerceIn(250, 1800).toLong()

        when (unlockState.phase) {
            FaceUnlockPhase.IDLE -> {
                isVisible = false
                isSuccess = false
                isInterrupted = false
                progress.snapTo(0f)
            }

            FaceUnlockPhase.SCANNING -> {
                isInterrupted = false
                isSuccess = false
                isVisible = true
                progress.snapTo(0.05f)

                if (unlockState.provenance == DetectionProvenance.MANUAL_TEST) {
                    // Séquence de démonstration contrôlée : balayage puis validation complète
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = scanDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                    isSuccess = true
                    onSuccessReached()
                    delay(holdMs)
                    isVisible = false
                    progress.snapTo(0f)
                    onCompleted()
                } else {
                    // En écoute réelle (écran verrouillé) : progresse vers 82% en attendant USER_PRESENT
                    progress.animateTo(
                        targetValue = 0.82f,
                        animationSpec = tween(
                            durationMillis = scanDuration,
                            easing = LinearEasing
                        )
                    )
                }
            }

            FaceUnlockPhase.SUCCESS -> {
                isInterrupted = false
                isVisible = true
                val remainingMs = ((1f - progress.value) * (scanDuration * 0.45f)).toInt().coerceIn(140, 420)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingMs,
                        easing = FastOutSlowInEasing
                    )
                )
                isSuccess = true
                onSuccessReached()
                delay(holdMs)
                isVisible = false
                isSuccess = false
                progress.snapTo(0f)
                onCompleted()
            }

            FaceUnlockPhase.INTERRUPTED -> {
                isSuccess = false
                isInterrupted = true
                isVisible = true
                val target = progress.value.coerceAtLeast(0.35f)
                progress.snapTo(target)
                delay(320L)
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                )
                isVisible = false
                isInterrupted = false
                onCompleted()
            }
        }
    }
}

@Composable
fun rememberFaceAnimationController(
    unlockState: FaceUnlockState,
    settings: FaceAnimationSettings,
    onCompleted: () -> Unit = {},
    onSuccessReached: () -> Unit = {}
): FaceAnimationController {
    val controller = remember { FaceAnimationController() }
    LaunchedEffect(
        unlockState.sequenceId,
        unlockState.phase,
        unlockState.isScreenOn,
        settings.enabled,
        settings.speedMultiplier,
        settings.durationMs
    ) {
        controller.runForState(
            unlockState = unlockState,
            settings = settings,
            onCompleted = onCompleted,
            onSuccessReached = onSuccessReached
        )
    }
    return controller
}
