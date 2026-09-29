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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookCard(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isGrid: Boolean = true,
    imageUrl: String? = null,
    progress: Float = 0f
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
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
                    imageUrl = imageUrl
                )

                Column(
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                style = MaterialTheme.typography.labelMedium
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
                                text = "Progress",
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )

                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
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
                BookCover(
                    imageUrl = imageUrl,
                    modifier = Modifier.weight(.3f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            minLines = 2,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontWeight = FontWeight.Normal,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
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
                                text = "Progress",
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )

                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
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
