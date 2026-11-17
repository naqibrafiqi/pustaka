package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString

/** Books whose file has gone are shown faded. */
private const val DIMMED_ALPHA = 0.5f

/** The cover's width in the list layout. */
private val LIST_COVER_WIDTH = 96.dp

/** A card for [book]. The subtitle is its volume label if it has one, otherwise its credits. */
@Composable
fun BookCard(
    book: BookItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGrid: Boolean = true,
    subtitle: String? = book.volumeLabel?.asString() ?: book.credits,
) {
    BookCard(
        onClick = onClick,
        title = book.title,
        modifier = modifier,
        subtitle = subtitle,
        isGrid = isGrid,
        imageUrl = book.coverPath,
        progress = book.progress,
        statusLabel = stringResource(book.state.label),
        isDimmed = book.isMissing,
    )
}

@Composable
fun BookCard(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isGrid: Boolean = true,
    imageUrl: String? = null,
    progress: Float = 0f,
    statusLabel: String = stringResource(R.string.book_state_unread),
    isDimmed: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDimmed) DIMMED_ALPHA else 1f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isGrid) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BookCover(
                    imageUrl = imageUrl,
                    modifier = Modifier.padding(
                        start = 8.dp,
                        top = 16.dp,
                        end = 8.dp
                    )
                )

                Column(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                        end = 8.dp
                    )
                ) {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontWeight = FontWeight.Normal,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusLabel,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Text(
                                text = stringResource(R.string.common_percent, (progress * 100).toInt()),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            gapSize = 0.dp
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // About the size it always was on a phone, and no bigger on a tablet.
                BookCover(
                    imageUrl = imageUrl,
                    modifier = Modifier.width(LIST_COVER_WIDTH)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontWeight = FontWeight.Normal,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusLabel,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Text(
                                text = stringResource(R.string.common_percent, (progress * 100).toInt()),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            gapSize = 0.dp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardPreview() {
    PustakaTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BookCard(
                onClick = {},
                title = "The Stories Of Someone Life That Is Too Long To Be A Book",
                subtitle = "Author Name",
                modifier = Modifier.width(180.dp),
                isGrid = true,
                progress = .699f
            )

            BookCard(
                onClick = {},
                title = "The Stories Of Someone Life That Is Too Long To Be A Book",
                subtitle = "Author Name",
                isGrid = false,
                progress = .699f
            )
        }
    }
}
