package my.naqibrafiqi.pustaka.ui.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupScreenTest {

    @Test
    fun `first launch shows the welcome screen`() {
        assertEquals(StartupScreen.WELCOME, StartupScreen.of(hasAccess = false, hasSeenWelcome = false))
    }

    @Test
    fun `a refusal after the welcome screen shows the grant access screen`() {
        assertEquals(StartupScreen.ACCESS_NEEDED, StartupScreen.of(hasAccess = false, hasSeenWelcome = true))
    }

    @Test
    fun `access shows the library, whether or not the welcome screen was seen`() {
        assertEquals(StartupScreen.READY, StartupScreen.of(hasAccess = true, hasSeenWelcome = true))
        assertEquals(StartupScreen.READY, StartupScreen.of(hasAccess = true, hasSeenWelcome = false))
    }
}
