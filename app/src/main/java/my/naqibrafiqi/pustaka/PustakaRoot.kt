package my.naqibrafiqi.pustaka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.naqibrafiqi.pustaka.ui.feature.onboarding.AccessNeededScreen
import my.naqibrafiqi.pustaka.ui.feature.onboarding.StartupScreen
import my.naqibrafiqi.pustaka.ui.feature.onboarding.StartupViewModel
import my.naqibrafiqi.pustaka.ui.feature.onboarding.WelcomeScreen
import my.naqibrafiqi.pustaka.ui.feature.onboarding.rememberStorageAccessRequest

/**
 * Shows the library once the app has access to the files, and the first-launch screens until then.
 * Access is checked again every time the app comes back, since it can be turned off in the system
 * settings at any time.
 */
@Composable
fun PustakaRoot(viewModel: StartupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshAccess()
        onPauseOrDispose { }
    }

    val requestAccess = rememberStorageAccessRequest(onResult = viewModel::refreshAccess)

    when (state.screen) {
        StartupScreen.LOADING -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        StartupScreen.WELCOME -> WelcomeScreen(
            libraryFolderPath = state.libraryFolderPath,
            onContinue = {
                viewModel.onWelcomeSeen()
                requestAccess()
            },
        )
        StartupScreen.ACCESS_NEEDED -> AccessNeededScreen(
            libraryFolderPath = state.libraryFolderPath,
            onGrantAccess = requestAccess,
        )
        StartupScreen.READY -> PustakaApp()
    }
}
