package io.github.anshireminder.app.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class CurrentDateInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun returningFromBackgroundRefreshesDateWithoutInteraction() {
        var providedDate = LocalDate.of(2026, 9, 26)
        val lifecycleOwner = TestLifecycleOwner()

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val currentDate = rememberCurrentDate(
                    currentDateProvider = { providedDate },
                    currentDateTimeProvider = {
                        providedDate.atTime(12, 0).atZone(ZoneId.of("Europe/Paris"))
                    },
                )
                Text(currentDate.toString())
            }
        }

        composeRule.runOnIdle {
            lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        }
        composeRule.onNodeWithText("2026-09-26").assertIsDisplayed()

        composeRule.runOnIdle {
            lifecycleOwner.registry.currentState = Lifecycle.State.STARTED
            providedDate = LocalDate.of(2026, 9, 27)
            lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        }

        composeRule.onNodeWithText("2026-09-27").assertIsDisplayed()
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
