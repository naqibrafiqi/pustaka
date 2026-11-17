package my.naqibrafiqi.pustaka.ui.feature.onboarding

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/** What the user does next, in the words of the system screen they'll see. */
private val accessInstruction: Int
    @StringRes get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        R.string.access_instruction_all_files
    } else {
        R.string.access_instruction_legacy
    }

/** Shown on first launch, before the permission screen. */
@Composable
fun WelcomeScreen(
    libraryFolderPath: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccessScreenLayout(
        iconResId = R.drawable.local_library,
        title = stringResource(R.string.welcome_title),
        body = stringResource(R.string.welcome_body, stringResource(accessInstruction)),
        libraryFolderPath = libraryFolderPath,
        buttonLabel = stringResource(R.string.welcome_continue),
        onButtonClick = onContinue,
        modifier = modifier,
    )
}

/** Shown when access was refused, or was taken away in the system settings. */
@Composable
fun AccessNeededScreen(
    libraryFolderPath: String,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccessScreenLayout(
        iconResId = R.drawable.folder_managed,
        title = stringResource(R.string.access_needed_title),
        body = stringResource(R.string.access_needed_body, stringResource(accessInstruction)),
        libraryFolderPath = libraryFolderPath,
        buttonLabel = stringResource(R.string.access_grant),
        onButtonClick = onGrantAccess,
        modifier = modifier,
    )
}

@Composable
private fun AccessScreenLayout(
    @DrawableRes iconResId: Int,
    title: String,
    body: String,
    libraryFolderPath: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .centredContentWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.settings_library_folder),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = libraryFolderPath,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = buttonLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    PustakaTheme {
        WelcomeScreen(libraryFolderPath = "/storage/emulated/0/Pustaka", onContinue = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessNeededScreenPreview() {
    PustakaTheme {
        AccessNeededScreen(libraryFolderPath = "/storage/emulated/0/Pustaka", onGrantAccess = {})
    }
}
