package my.naqibrafiqi.pustaka.ui.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenBrightnessTest {

    @Test
    fun followsTheSameCurveAsThePhonesSlider() {
        // Halfway along Android's slider is a twelfth of full brightness.
        assertEquals(1f / 12f, ScreenBrightness.toWindowBrightness(0.5f), 1e-4f)
        assertEquals(1f, ScreenBrightness.toWindowBrightness(1f), 1e-4f)
        assertEquals(0.5f, ScreenBrightness.toLevel(1f / 12f), 1e-4f)
    }

    @Test
    fun turnsBackIntoTheSameSliderPosition() {
        for (level in listOf(0.1f, 0.3f, 0.5f, 0.75f, 0.9f, 1f)) {
            assertEquals(level, ScreenBrightness.toLevel(ScreenBrightness.toWindowBrightness(level)), 1e-4f)
        }
    }

    @Test
    fun theBottomOfTheSliderDoesntTurnTheScreenOff() {
        assertTrue(ScreenBrightness.toWindowBrightness(0f) > 0f)
        assertTrue(ScreenBrightness.toWindowBrightness(-1f) > 0f)
        assertEquals(1f, ScreenBrightness.toWindowBrightness(2f), 0f)
    }
}
