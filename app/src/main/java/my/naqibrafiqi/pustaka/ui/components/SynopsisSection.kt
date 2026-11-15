package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/**
 * Genre chips above a synopsis that expands when tapped. While collapsed, the chips scroll in one
 * row; expanded, they wrap. Either part is left out when there's nothing to show.
 *
 * @param horizontalPadding the space at the screen's edges, so the chip row can scroll to the edge.
 */
@Composable
fun SynopsisSection(
    genres: List<String>,
    synopsis: String?,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    if (genres.isEmpty() && synopsis.isNullOrBlank()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    // Without a synopsis to tap, the chips always wrap so none of them are hidden.
    val wrapChips = expanded || synopsis.isNullOrBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (genres.isNotEmpty()) {
            if (wrapChips) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        GenreChip(text = genre)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING)
                ) {
                    // The row's own spacing adds 8dp next to each edge spacer.
                    val edge = horizontalPadding - CHIP_SPACING
                    if (edge > 0.dp) Spacer(modifier = Modifier.width(edge))

                    genres.forEach { genre ->
                        GenreChip(text = genre)
                    }

                    if (edge > 0.dp) Spacer(modifier = Modifier.width(edge))
                }
            }
        }

        if (!synopsis.isNullOrBlank()) {
            Surface(
                onClick = {
                    expanded = !expanded
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(horizontal = horizontalPadding)),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = synopsis,
                        fontWeight = FontWeight.Light,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = stringResource(if (expanded) R.string.common_label_read_less else R.string.common_label_read_more),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private const val COLLAPSED_LINES = 4

private val CHIP_SPACING = 8.dp

@Preview(showBackground = true)
@Composable
private fun SynopsisSectionPreview() {
    PustakaTheme {
        SynopsisSection(
            genres = listOf("Drama", "Fantasy", "Action"),
            synopsis = "A long description of the book, read from its content.opf file. ".repeat(6),
        )
    }
}
