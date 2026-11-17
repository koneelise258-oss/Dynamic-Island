package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mike.dynamicisland.MikeApplication
import com.mike.dynamicisland.utils.DeviceProfileDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `application name and engine initialize cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Mike Dynamic Island", context.getString(R.string.app_name))

        val profile = DeviceProfileDetector.detect(context)
        assertTrue(profile.screenWidth >= 360)
        assertTrue(profile.screenHeight >= 640)

        val engine = MikeApplication.getEngine(context)
        assertTrue(engine.eventManager.events.value.isNotEmpty())
    }
}
