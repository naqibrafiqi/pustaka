package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Keeps a bottom sheet still when it's flung upwards while already as high as it goes (#31). Put
 * it on the sheet's content, around anything that scrolls.
 *
 * Material's sheet settles with a spring that keeps the speed of the fling, so an upward fling
 * the content can't use sends the sheet past its place and back, and it shakes. That leftover
 * speed is used up here instead. Swipes down still reach the sheet, so it can be swiped closed.
 */
fun Modifier.steadyBottomSheet(): Modifier = nestedScroll(UseUpUpwardFling)

/**
 * Passes drags on a part of the sheet that doesn't scroll, such as its title, through
 * [steadyBottomSheet] like the rest of the content. Otherwise they go straight to the sheet.
 */
@Composable
fun Modifier.sheetDragArea(): Modifier = scrollable(rememberScrollableState { 0f }, Orientation.Vertical)

private object UseUpUpwardFling : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
}
