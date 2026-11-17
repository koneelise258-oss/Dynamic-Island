package com.mike.dynamicisland

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mike.dynamicisland.faceid.DetectionProvenance
import com.mike.dynamicisland.faceid.FaceUnlockEngine
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.faceid.UnlockReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FaceUnlockAndReceiverTest {

    @Test
    fun `screen on while keyguard locked starts scanning and screen off interrupts immediately`() {
        var clock = 1000L
        val logs = mutableListOf<Triple<String, String, Boolean>>()
        val engine = FaceUnlockEngine(
            timeProvider = { clock },
            onEventLogged = { c, m, r -> logs.add(Triple(c, m, r)) }
        )

        engine.onScreenOn(keyguardLocked = true)
        assertEquals(FaceUnlockPhase.SCANNING, engine.state.value.phase)
        assertTrue(engine.state.value.isScreenOn)
        assertTrue(engine.state.value.isKeyguardLocked)
        assertEquals(DetectionProvenance.SYSTEM_SCREEN_ON_LOCKED, engine.state.value.provenance)

        clock = 1450L
        engine.onScreenOff()
        assertEquals(FaceUnlockPhase.INTERRUPTED, engine.state.value.phase)
        assertFalse(engine.state.value.isScreenOn)
        assertEquals(DetectionProvenance.SYSTEM_SCREEN_OFF_INTERRUPTED, engine.state.value.provenance)
        assertTrue(logs.all { it.third })
    }

    @Test
    fun `user present validates in honest degraded keyguard mode with elapsed latency`() {
        var clock = 5000L
        val engine = FaceUnlockEngine(timeProvider = { clock })

        engine.onScreenOn(keyguardLocked = true)
        clock = 5620L
        engine.onUserPresent(allowDegradedUnlockAnimation = true)

        val state = engine.state.value
        assertEquals(FaceUnlockPhase.SUCCESS, state.phase)
        assertFalse(state.isKeyguardLocked)
        assertEquals(620L, state.unlockElapsedMs)
        assertEquals(DetectionProvenance.SYSTEM_USER_PRESENT_DEGRADED, state.provenance)
        assertFalse(state.capability.hasVendorDirectFaceApi)
    }

    @Test
    fun `user present does not trigger visual validation when degraded mode is disabled`() {
        val engine = FaceUnlockEngine(timeProvider = { 100L })
        engine.onScreenOn(keyguardLocked = true)
        engine.onUserPresent(allowDegradedUnlockAnimation = false)
        assertEquals(FaceUnlockPhase.IDLE, engine.state.value.phase)
    }

    @Test
    fun `unlock receiver dispatches screen on, user present, and screen off broadcasts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        shadowOf(km).setKeyguardLocked(true)

        val engine = FaceUnlockEngine()
        val receiver = UnlockReceiver(engine)

        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))
        assertEquals(FaceUnlockPhase.SCANNING, engine.state.value.phase)

        shadowOf(km).setKeyguardLocked(false)
        receiver.onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        assertEquals(FaceUnlockPhase.SUCCESS, engine.state.value.phase)

        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertFalse(engine.state.value.isScreenOn)
    }
}
