package com.mike.dynamicisland

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import com.mike.dynamicisland.battery.BatteryEventReceiver
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.BatteryEventType
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.notifications.NotificationController
import com.mike.dynamicisland.notifications.StoredNotificationEntry
import com.mike.dynamicisland.quickglass.QuickGlassConfig
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.settings.PrivacySettings
import com.mike.dynamicisland.utils.DeviceProfile
import com.mike.dynamicisland.utils.PerformanceTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SystemModulesRobolectricTest {

    @Test
    fun `tecno camon 15 air profile configures eco low ram and disables Android 12 hardware blur`() {
        val camon15Air = DeviceProfile(
            androidVersion = 29, // Android 10
            totalRamMb = 3072L,
            screenWidth = 720,
            screenHeight = 1600,
            refreshRate = 60f,
            hasDisplayCutout = false,
            densityDpi = 320,
            manufacturer = "TECNO",
            model = "TECNO CD6"
        )

        assertTrue(camon15Air.isLowRamProfile)
        assertTrue(camon15Air.isTecnoOrHiOsDevice)
        assertEquals(PerformanceTier.ECO_LOW_RAM, camon15Air.performanceTier)
        assertFalse(camon15Air.supportsHardwareBlur)
        assertEquals(8, camon15Air.maxParticleCount)
        assertEquals(96, camon15Air.maxAlbumArtSizePx)

        // Caméra punch-hole par défaut en haut à gauche sur Camon 15 Air
        val punchHole = camon15Air.recommendedPunchHoleBoundsPx()
        assertTrue(punchHole.centerXPx < camon15Air.screenWidth / 3)

        val resolvedGlass = QuickGlassEngine.resolveEffectiveConfig(
            raw = QuickGlassConfig(optionalHardwareBlurEnabled = true, haloIntensity = 0.85f),
            profile = camon15Air
        )
        assertFalse(resolvedGlass.optionalHardwareBlurEnabled)
        assertTrue(resolvedGlass.haloIntensity <= 0.45f)
    }

    @Test
    fun `notification controller groups whatsapp messages and respects privacy lockscreen mask`() {
        val activityManager = MikeActivityManager()
        val controller = NotificationController(activityManager, EventManager())

        controller.updateFilters(
            allowedPackages = setOf("com.whatsapp"),
            privacySettings = PrivacySettings(
                hideNotificationContentWhenLocked = true,
                privacyModeMaskPreview = false
            ),
            deviceLocked = false
        )

        controller.ingestEntry(
            StoredNotificationEntry(
                key = "wa_1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Karim",
                text = "On se retrouve à 18h ?",
                postedAtMs = 1000L,
                isOngoing = false,
                category = Notification.CATEGORY_MESSAGE,
                actions = emptyList(),
                actionIntents = emptyList(),
                contentIntent = null,
                progress = 0,
                progressMax = 0,
                progressIndeterminate = false
            )
        )
        controller.ingestEntry(
            StoredNotificationEntry(
                key = "wa_2",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Karim",
                text = "J'arrive dans 5 min",
                postedAtMs = 2000L,
                isOngoing = false,
                category = Notification.CATEGORY_MESSAGE,
                actions = emptyList(),
                actionIntents = emptyList(),
                contentIntent = null,
                progress = 0,
                progressMax = 0,
                progressIndeterminate = false
            )
        )

        val grouped = activityManager.activities.value["notif_com.whatsapp"] as? IslandActivity.Notification
        assertNotNull(grouped)
        assertEquals(2, grouped?.groupedCount)
        assertEquals("J'arrive dans 5 min", grouped?.message)
        assertFalse(grouped?.isContentMasked ?: true)

        // Verrouillage : le mode confidentialité masque immédiatement le texte
        controller.updateFilters(
            allowedPackages = setOf("com.whatsapp"),
            privacySettings = PrivacySettings(hideNotificationContentWhenLocked = true),
            deviceLocked = true
        )
        val masked = activityManager.activities.value["notif_com.whatsapp"] as? IslandActivity.Notification
        assertTrue(masked?.isContentMasked == true)
        assertFalse(masked?.message?.contains("J'arrive") == true)
    }

    @Test
    fun `battery receiver emits charger connected banner and auto hides`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityManager = MikeActivityManager()
        val receiver = BatteryEventReceiver(
            scope = backgroundScope,
            activityManager = activityManager,
            eventManager = EventManager(),
            bannerDurationMs = 4000L
        )

        val batteryLevelIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 74)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)
        }
        receiver.onReceive(context, batteryLevelIntent)
        receiver.onReceive(context, Intent(Intent.ACTION_POWER_CONNECTED))
        runCurrent()

        val batteryActivity = activityManager.activities.value.values
            .firstOrNull { it.kind == ActivityKind.BATTERY } as? IslandActivity.Battery
        assertNotNull(batteryActivity)
        assertEquals(BatteryEventType.CHARGER_CONNECTED, batteryActivity?.eventType)
        assertEquals(74, batteryActivity?.levelPercent)

        advanceTimeBy(4500L)
        runCurrent()
        val afterHide = activityManager.activities.value.values.firstOrNull { it.kind == ActivityKind.BATTERY }
        assertNull(afterHide)
    }
}
