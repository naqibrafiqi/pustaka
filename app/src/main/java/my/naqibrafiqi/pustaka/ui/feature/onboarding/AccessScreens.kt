package my.naqibrafiqi.pustaka.ui.feature.onboarding

import android.os.Build
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/** What the user does next, in the words of the system screen they'll see. */
private val accessInstruction: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "turn on \"Allow access to manage all files\" for Pustaka."
    } else {
        "allow Pustaka to access files on your device."
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
        title = "Welcome to Pustaka",
        body = "Pustaka shows the EPUB books you keep in one folder on your phone. " +
            "To read that folder, it needs access to your files. Next, $accessInstruction\n\n" +
            "Pustaka never renames or moves your files, and only deletes them when you ask it to.",
        libraryFolderPath = libraryFolderPath,
        buttonLabel = "Continue",
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
        title = "Pustaka needs access to your files",
        body = "Without it, Pustaka can't find the books in your library folder. " +
            "Tap Grant access and $accessInstruction",
        libraryFolderPath = libraryFolderPath,
        buttonLabel = "Grant access",
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
                    text = "Library folder",
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
