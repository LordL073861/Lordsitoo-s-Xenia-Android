package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.EmptyLibraryState
import com.example.ui.theme.XeniaAndroidTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun library_empty_screenshot() {
    composeTestRule.setContent {
      XeniaAndroidTheme {
        EmptyLibraryState(onAddFolderClick = {}, hasSearch = false)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/library_empty.png")
  }
}
