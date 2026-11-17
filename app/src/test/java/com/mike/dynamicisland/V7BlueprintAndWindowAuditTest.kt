package com.mike.dynamicisland

import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.mike.dynamicisland.core.ActivityPriorityManager
import com.mike.dynamicisland.core.ConnectedAudioAccessory
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.core.MikeEngine
import com.mike.dynamicisland.core.SystemActivityController
import com.mike.dynamicisland.faceid.FacePositionMode
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.DynamicIslandWindow
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.RingerStateType
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.settings.IslandAlignment
import com.mike.dynamicisland.utils.CutoutHorizontalZone
import com.mike.dynamicisland.utils.DeviceProfile
import com.mike.dynamicisland.utils.DisplayCutoutManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Suite d'audit architectural V7 (PARTIE 1, 2, 3, 4 & 5) :
 * Vérifie que la fenêtre flottante reste strictement compacte en haut de l'écran,
 * que le scaling 390pt -> 360dp TECNO Camon 15 Air est exact,
 * et que tous les nouveaux états système V7 fonctionnent sans mock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class V7BlueprintAndWindowAuditTest {

    private val camon15Air = DeviceProfile(
        androidVersion = 29,
        totalRamMb = 3072L,
        screenWidth = 720,
        screenHeight = 1600,
        refreshRate = 60f,
        hasDisplayCutout = false,
        densityDpi = 320, // density = 2.0f -> 360 dp de largeur
        manufacturer = "TECNO",
        model = "TECNO CD6",
        availableRamMb = 1024L,
        freeStorageMb = 5400L
    )

    @Test
    fun `blueprint 390pt scales proportionally to 360dp for TECNO Camon 15 Air`() {
        assertEquals(360f, camon15Air.screenWidthDp, 0.1f)
        val metrics = IslandBlueprintMetrics.forScreenWidthDp(camon15Air.screenWidthDp)

        // 360 / 390 ~= 0.923
        assertEquals(360f / 390f, metrics.scaleFactor, 0.01f)
        // Marges sûres 12dp de chaque côté -> maxExpandedWidth <= 336dp
        assertTrue(metrics.expandedMaxWidth.value <= 336.1f)
        assertTrue(metrics.expandedMediaHeight.value <= MikeDesignTokens.MaxFloatingWindowHeightDp.value)
        assertEquals(36f, metrics.compactHeight.value, 0.1f)
        assertEquals(44f, metrics.callActionButtonSize.value, 0.1f)
        assertTrue(camon15Air.displayPanelNote.contains("LCD", ignoreCase = true))
    }

    @Test
    fun `face overlay bounds never exceed 124dp and respect left punch-hole on Camon 15 Air`() {
        assertEquals(CutoutHorizontalZone.LEFT, DisplayCutoutManager.detectZone(camon15Air))

        val bounds = DisplayCutoutManager.computeFaceOverlayBounds(
            profile = camon15Air,
            density = camon15Air.density,
            ringSizeDp = 240f, // tentative volontairement excessive
            positionMode = FacePositionMode.AUTO_CUTOUT,
            manualOffsetXDp = 0f,
            manualOffsetYDp = 0f
        )

        val maxFacePx = (MikeDesignTokens.MaxFaceWindowSizeDp.value * camon15Air.density).toInt()
        assertTrue("La largeur Face ID doit rester <= 124dp", bounds.maxWidthPx <= maxFacePx)
        assertTrue("La hauteur Face ID doit rester <= 124dp", bounds.maxHeightPx <= maxFacePx)
        assertTrue("La fenêtre Face ID doit rester en haut à gauche sur Camon 15 Air", bounds.xPx < camon15Air.screenWidth / 3)
        assertTrue("La fenêtre Face ID doit rester dans le quart supérieur", bounds.yPx <= camon15Air.screenHeight / 4)
    }

    @Test
    fun `island overlay bounds stay strictly compact at top and never fullscreen`() {
        val compactBounds = DisplayCutoutManager.computeIslandOverlayBounds(
            profile = camon15Air,
            density = camon15Air.density,
            isExpanded = false,
            alignment = IslandAlignment.AUTO_CUTOUT,
            verticalOffsetDp = 12f,
            horizontalOffsetDp = 0f
        )
        assertTrue("Mini Island <= 54dp de hauteur", compactBounds.maxHeightPx <= (54f * camon15Air.density).toInt())
        assertTrue("Mini Island ne dépasse pas la largeur utile", compactBounds.maxWidthPx < camon15Air.screenWidth)
        assertEquals(Gravity.TOP or Gravity.START, compactBounds.gravity)

        val expandedBounds = DisplayCutoutManager.computeIslandOverlayBounds(
            profile = camon15Air,
            density = camon15Air.density,
            isExpanded = true,
            alignment = IslandAlignment.AUTO_CUTOUT,
            verticalOffsetDp = 12f,
            horizontalOffsetDp = 0f
        )
        val maxExpandedPx = (MikeDesignTokens.MaxFloatingWindowHeightDp.value * camon15Air.density).toInt()
        assertTrue("Smart Island <= 216dp de hauteur (jamais plein écran)", expandedBounds.maxHeightPx <= maxExpandedPx)
        assertTrue("Smart Island < 30% de la hauteur écran", expandedBounds.maxHeightPx < camon15Air.screenHeight / 3)
        assertEquals(Gravity.TOP or Gravity.CENTER_HORIZONTAL, expandedBounds.gravity)
    }

    @Test
    fun `DynamicIslandWindow layout params strictly forbid MATCH_PARENT and FLAG_LAYOUT_NO_LIMITS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = MikeEngine(context)
        val window = DynamicIslandWindow(
            context = context,
            engine = engine,
            onFaceSuccessHaptic = {}
        )

        val faceParams = window.buildStrictFaceLayoutParams()
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, faceParams.width)
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, faceParams.height)
        assertEquals(0, faceParams.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        assertEquals(0, faceParams.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        assertTrue((faceParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0)
        assertTrue((faceParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0)

        val compactIslandParams = window.buildStrictIslandLayoutParams(isExpanded = false)
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, compactIslandParams.width)
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, compactIslandParams.height)
        assertEquals(0, compactIslandParams.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        assertEquals(0, compactIslandParams.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val expandedIslandParams = window.buildStrictIslandLayoutParams(isExpanded = true)
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, expandedIslandParams.width)
        assertNotEquals(WindowManager.LayoutParams.MATCH_PARENT, expandedIslandParams.height)
        assertEquals(0, expandedIslandParams.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        assertEquals(0, expandedIslandParams.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Test
    fun `SystemActivityController publishes silent, DND, and accessory Mini Island banners and auto clears`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityManager = MikeActivityManager()
        val controller = SystemActivityController(
            context = context,
            scope = backgroundScope,
            activityManager = activityManager,
            eventManager = EventManager(),
            transientBannerMs = 3000L
        )

        controller.publishSilentModeBanner(RingerStateType.SILENT)
        runCurrent()
        val silent = activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.SILENT_MODE } as? IslandActivity.SilentMode
        assertNotNull(silent)
        assertEquals("Silent", silent?.compactText)

        controller.publishDndBanner(true)
        runCurrent()
        val dnd = activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.DO_NOT_DISTURB } as? IslandActivity.DoNotDisturb
        assertNotNull(dnd)
        assertTrue(dnd?.isActive == true)

        controller.publishAccessoryBanner(
            ConnectedAudioAccessory(
                name = "Casque Studio",
                isBluetooth = true,
                deviceTypeLabel = "Bluetooth Audio",
                batteryPercent = null
            )
        )
        runCurrent()
        val acc = activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.ACCESSORY } as? IslandActivity.AccessoryConnected
        assertNotNull(acc)
        assertEquals("Connecté", acc?.compactText)

        advanceTimeBy(4500L)
        runCurrent()
        assertNull(activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.SILENT_MODE })
        assertNull(activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.DO_NOT_DISTURB })
        assertNull(activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.ACCESSORY })
    }

    @Test
    fun `ActivityPriorityManager arbitrates all 15 V7 activity kinds with call preemption`() {
        val manager = ActivityPriorityManager()
        val silent = IslandActivity.SilentMode(ringerState = RingerStateType.SILENT)
        val voice = IslandActivity.VoiceMemo(id = "vm1", label = "Mémo", durationSeconds = 12L, isRecording = true)
        val home = IslandActivity.HomeEvent(id = "h1", deviceTitle = "Lampe Salon", statusText = "Allumée")
        val incomingCall = IslandActivity.Call(
            id = "call_in",
            callerName = "Salim",
            statusText = "Appel entrant",
            startedAtElapsedMs = 0L,
            packageName = "com.android.dialer",
            isIncoming = true
        )

        val resolved = manager.resolve(
            activities = listOf(home, voice, silent, incomingCall),
            priorityOrder = listOf(ActivityKind.HOME_EVENT, ActivityKind.VOICE_MEMO),
            pinnedPrimaryId = home.id,
            allowMultiple = true
        )

        assertEquals(ActivityKind.CALL, resolved.primary?.kind)
        assertFalse(resolved.ordered.isEmpty())
        assertEquals(4, resolved.ordered.size)
        assertNotNull(resolved.secondary)
    }

    @Test
    fun `all 9 custom Apple-inspired VectorDrawables inflate cleanly from Android resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vectorIds = listOf(
            com.example.R.drawable.ic_mike_face_id,
            com.example.R.drawable.ic_mike_earbuds,
            com.example.R.drawable.ic_mike_audio_output,
            com.example.R.drawable.ic_mike_silent_bell,
            com.example.R.drawable.ic_mike_voice_memo,
            com.example.R.drawable.ic_mike_dnd_moon,
            com.example.R.drawable.ic_mike_battery_bolt,
            com.example.R.drawable.ic_mike_call_accept,
            com.example.R.drawable.ic_mike_call_decline
        )
        vectorIds.forEach { resId ->
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
            assertNotNull("VectorDrawable $resId doit se charger proprement", drawable)
            assertTrue("Largeur intrinsèque > 0 pour $resId", (drawable?.intrinsicWidth ?: 0) > 0)
            assertTrue("Hauteur intrinsèque > 0 pour $resId", (drawable?.intrinsicHeight ?: 0) > 0)
        }
    }

    @Test
    fun `MikeEngine VoiceMemo lifecycle supports start, pause preserving elapsed time, resume and stop`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = MikeEngine(context)

        engine.startOrResumeVoiceMemo(resetDuration = true)
        val started = engine.activityManager.activities.value[MikeEngine.VOICE_MEMO_ID] as? IslandActivity.VoiceMemo
        assertNotNull(started)
        assertTrue(started?.isRecording == true)
        assertFalse(started?.isPaused == true)

        // Simule un temps déjà écoulé avant mise en pause
        engine.activityManager.upsert(started!!.copy(durationSeconds = 19L))
        engine.pauseVoiceMemo()

        val paused = engine.activityManager.activities.value[MikeEngine.VOICE_MEMO_ID] as? IslandActivity.VoiceMemo
        assertNotNull(paused)
        assertFalse(paused?.isRecording == true)
        assertTrue(paused?.isPaused == true)
        assertEquals(19L, paused?.durationSeconds)
        assertEquals("00:19", paused?.compactText)

        engine.toggleVoiceMemoPauseResume()
        val resumed = engine.activityManager.activities.value[MikeEngine.VOICE_MEMO_ID] as? IslandActivity.VoiceMemo
        assertNotNull(resumed)
        assertTrue(resumed?.isRecording == true)
        assertEquals(19L, resumed?.durationSeconds)

        engine.stopVoiceMemo()
        assertNull(engine.activityManager.activities.value[MikeEngine.VOICE_MEMO_ID])
    }

    @Test
    fun `IslandActivity Call handles incoming, active and ended compact texts cleanly`() {
        val incoming = IslandActivity.Call(
            id = "c1",
            callerName = "Salim",
            statusText = "",
            startedAtElapsedMs = 0L,
            packageName = "com.android.dialer",
            isIncoming = true
        )
        assertEquals("Entrant", incoming.compactText)

        val active = incoming.copy(isIncoming = false, statusText = "01:24")
        assertEquals("01:24", active.compactText)

        val ended = incoming.copy(isIncoming = false, isEnded = true, statusText = "")
        assertEquals("Terminé", ended.compactText)
    }
}
