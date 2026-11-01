package com.rangelreale.tlptclock.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.rangelreale.tlptclock.clock.ClockViewModel
import com.rangelreale.tlptclock.clock.MS_PER_MINUTE
import com.rangelreale.tlptclock.clock.PreferencesClockStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-xxhdpi")
// The length dialog has no test here: its text field never goes idle under Robolectric. It was
// checked on an emulator instead.
class ClockScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var time = 0L

    private fun show(): ClockViewModel {
        val store = PreferencesClockStore(ApplicationProvider.getApplicationContext<Application>())
        val vm = ClockViewModel(store) { time }
        compose.setContent {
            val ui by vm.ui.collectAsState()
            ClockTheme { ClockScreen(ui, vm) }
        }
        return vm
    }

    @Test
    fun `presets and steppers change the countdown with one tap`() {
        show()
        compose.onNodeWithText("20:00").assertExists()

        compose.onNodeWithContentDescription("10 minutes").performClick()
        compose.onNodeWithText("10:00").assertExists()

        compose.onNodeWithContentDescription("One minute longer").performClick()
        compose.onNodeWithText("11:00").assertExists()

        compose.onNodeWithContentDescription("One minute shorter").performClick()
        compose.onNodeWithText("10:00").assertExists()
    }

    @Test
    fun `start pause reset and next level work`() {
        val vm = show()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Pause").assertExists()

        time = MS_PER_MINUTE
        vm.tick()
        compose.onNodeWithText("19:00").assertExists()

        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Resume").assertExists()

        compose.onNodeWithText("Reset level").performClick()
        compose.onNodeWithText("20:00").assertExists()
        compose.onNodeWithText("Start").assertExists()

        compose.onNodeWithContentDescription("Next level").performClick()
        compose.onNodeWithText("Level 2").assertExists()
    }

    @Test
    fun `countdown resets by itself between blinds`() {
        val vm = show()
        compose.onNodeWithContentDescription("5 minutes").performClick()
        compose.onNodeWithText("Start").performClick()

        time = 5 * MS_PER_MINUTE
        vm.tick()
        compose.onNodeWithText("05:00").assertExists()
        compose.onNodeWithText("Blinds up: level 2").assertExists()
        compose.onNodeWithText("Pause").assertExists()
    }

    @Test
    fun `new game asks first`() {
        val vm = show()
        vm.nextLevel()
        compose.onNodeWithText("New game").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Level 2").assertExists()
    }

    @Test
    fun `portrait screenshot`() = screenshot("portrait")

    @Test
    fun `portrait running with little time screenshot`() {
        val vm = show()
        vm.setLengthMinutes(1)
        vm.startOrPause()
        time = 52_000
        vm.tick()
        save("portrait-urgent")
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun `landscape phone screenshot`() = screenshot("landscape")

    @Test
    @Config(qualifiers = "w640dp-h360dp-land-xhdpi")
    fun `small landscape phone screenshot`() = screenshot("landscape-small")

    @Test
    @Config(qualifiers = "w320dp-h568dp-port-xhdpi")
    fun `small portrait phone screenshot`() {
        val vm = show()
        vm.setLengthMinutes(120)
        save("portrait-small")
    }

    private fun screenshot(name: String) {
        show()
        save(name)
    }

    /** Writes a picture to app/build/screenshots so that a person can check the layout. */
    private fun save(name: String) {
        compose.waitForIdle()
        // captureToImage does not work under Robolectric, so this draws the view directly.
        val root = compose.onAllNodes(isRoot()).onLast().fetchSemanticsNode().root
        val view = (root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
