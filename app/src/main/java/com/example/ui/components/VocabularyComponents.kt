package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.Duration
import com.example.ui.theme.Elevation
import com.example.ui.theme.Spacing
import com.example.ui.theme.customColors

// 1. Staggered Animated List Item Container
@Composable
fun AnimatedListItem(
    index: Int,
    modifier: Modifier = Modifier,
    delayMultiplier: Int = 40,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * delayMultiplier).toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(Duration.medium, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(Duration.medium)),
        modifier = modifier
    ) {
        content()
    }
}

// 2. Custom Gradient Header (Premium visual background & typography)
@Composable
fun GradientHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    ),
    actionContent: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CornerRadius.large),
        shape = CornerRadius.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level3)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(Spacing.medium)
                .fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    if (actionContent != null) {
                        Row(content = actionContent)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}

// 3. Search Highlight Text Component (Custom styled highlight display)
@Composable
fun SearchHighlightText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    HighlightedText(
        text = text,
        query = query,
        modifier = modifier,
        style = style
    )
}

// 4. Primary Word Card
@Composable
fun WordCard(
    word: VocabularyWord,
    onWordClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPronounceClick: () -> Unit,
    modifier: Modifier = Modifier,
    query: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraSmall)
            .clickable(onClick = onWordClick)
            .semantics { contentDescription = "Vocabulary word: ${word.word}. Double tap to view details." },
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.medium)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic Tag
                word.topic?.let { topic ->
                    TopicChip(topic = topic)
                } ?: Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PronunciationButton(
                        onClick = onPronounceClick,
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                    BookmarkButton(
                        isBookmarked = word.isFavorite,
                        onClick = onBookmarkToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            // Main Word
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Pronunciation Phonetics
            word.pronunciation?.let { pronunciation ->
                Text(
                    text = pronunciation,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Meaning text with query highlighting
            SearchHighlightText(
                text = word.meaning,
                query = query,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Other forms preview if available
            if (!word.otherForms.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "Forms: ${word.otherForms}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

// 5. Expandable / Collapsible Card
@Composable
fun ExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    isExpandedInitially: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(isExpandedInitially) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraSmall),
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isExpanded) "COLLAPSE" else "EXPAND",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(Duration.medium)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(Duration.medium)) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                        .fillMaxWidth()
                ) {
                    content()
                }
            }
        }
    }
}

// 6. Meaning Detail Card
@Composable
fun MeaningCard(
    meaning: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "DEFINITION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            )
        }
    }
}

// 7. Example Detail Card (With stylish quote marker)
@Composable
fun ExampleCard(
    examples: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "CONTEXTUAL EXAMPLES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            
            examples.forEachIndexed { i, example ->
                Row(modifier = Modifier.padding(vertical = Spacing.extraSmall)) {
                    Text(
                        text = "“",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if (i < examples.size - 1) {
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                }
            }
        }
    }
}

// 8. Memory Hook / Mnemonic Device Card
@Composable
fun MemoryHookCard(
    hook: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.customColors.warningContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Mnemonic device tip",
                tint = MaterialTheme.customColors.warning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Column {
                Text(
                    text = "MEMORY HOOK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.customColors.warning,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = hook,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// 9. Premium Word of the Day Card
@Composable
fun WordOfDayCard(
    word: VocabularyWord,
    onWordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onWordClick)
            .semantics { contentDescription = "Word of the Day: ${word.word}" },
        shape = CornerRadius.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level2)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text(
                        text = "WORD OF THE DAY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
                
                TopicChip(
                    topic = word.topic ?: "UPSC General",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = word.word,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REVISE NOW",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }
}

// 10. Recent Search Query Card Row
@Composable
fun RecentSearchCard(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete search history entry",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 11. Category / Chapter Card
@Composable
fun ChapterCard(
    chapterName: String,
    wordCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraSmall)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Chapter $chapterName, containing $wordCount words." },
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = chapterName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Surface(
                    shape = CornerRadius.full,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = Spacing.small)
                ) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (progress > 0f) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CornerRadius.full),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

// 12. Random Discover / Quick revision card
@Composable
fun RandomWordCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Draw a random word from database" },
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
                Column {
                    Text(
                        text = "Quick Discovery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Draw an absolute random high-yield vocabulary word",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// 13. Historically Viewed Vocabulary Card
@Composable
fun HistoryCard(
    word: VocabularyWord,
    viewedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraSmall)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Viewed ${word.word} at $viewedTime" },
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.medium)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = viewedTime,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
        }
    }
}

// 14. Live Search Suggestion Item Card
@Composable
fun SearchSuggestionItem(
    suggestion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    query: String = ""
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.small, horizontal = Spacing.medium),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.medium))
            SearchHighlightText(
                text = suggestion,
                query = query,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
