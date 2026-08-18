package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.viewmodel.WordViewModel

sealed interface LibraryScreenState {
    object Dashboard : LibraryScreenState
    object ChapterList : LibraryScreenState
    data class ChapterDetail(val chapterName: String) : LibraryScreenState
    object AlphabeticalIndex : LibraryScreenState
    object TopicBrowser : LibraryScreenState
    data class WordList(val title: String, val listType: WordListType, val filterTopic: String? = null) : LibraryScreenState
}

@Composable
fun LibraryScreen(
    viewModel: WordViewModel,
    onNavigateToWord: (Int) -> Unit,
    onNavigateToChapter: (String) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateBack: () -> Unit,
    onStartSavedRevision: () -> Unit = {}
) {
    // Custom Navigation Stack inside Library screen for seamless nested page transitions
    val navigationStack = remember { mutableStateListOf<LibraryScreenState>(LibraryScreenState.Dashboard) }

    // Intercept Back Press to navigate gracefully inside Library before exiting
    BackHandler(enabled = true) {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        } else {
            onNavigateBack()
        }
    }

    Crossfade(
        targetState = navigationStack.lastOrNull() ?: LibraryScreenState.Dashboard,
        label = "LibraryScreenTransitions"
    ) { currentScreen ->
        when (currentScreen) {
            is LibraryScreenState.Dashboard -> {
                LibraryDashboard(
                    viewModel = viewModel,
                    onNavigateToSubScreen = { subScreen ->
                        navigationStack.add(subScreen)
                    },
                    onNavigateToWord = onNavigateToWord,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateBack = onNavigateBack
                )
            }
            is LibraryScreenState.ChapterList -> {
                ChapterListScreen(
                    viewModel = viewModel,
                    onNavigateToChapter = { chapterName ->
                        navigationStack.add(LibraryScreenState.ChapterDetail(chapterName))
                    },
                    onNavigateBack = {
                        navigationStack.removeLast()
                    }
                )
            }
            is LibraryScreenState.ChapterDetail -> {
                ChapterDetailScreen(
                    chapterName = currentScreen.chapterName,
                    viewModel = viewModel,
                    onNavigateToWord = onNavigateToWord,
                    onNavigateBack = {
                        navigationStack.removeLast()
                    }
                )
            }
            is LibraryScreenState.AlphabeticalIndex -> {
                AlphabeticalIndexScreen(
                    viewModel = viewModel,
                    onNavigateToWord = onNavigateToWord,
                    onNavigateBack = {
                        navigationStack.removeLast()
                    }
                )
            }
            is LibraryScreenState.TopicBrowser -> {
                TopicScreen(
                    viewModel = viewModel,
                    onNavigateToTopicWords = { topic ->
                        navigationStack.add(
                            LibraryScreenState.WordList(
                                title = topic,
                                listType = WordListType.BY_TOPIC,
                                filterTopic = topic
                            )
                        )
                    },
                    onNavigateBack = {
                        navigationStack.removeLast()
                    }
                )
            }
            is LibraryScreenState.WordList -> {
                WordListScreen(
                    title = currentScreen.title,
                    listType = currentScreen.listType,
                    filterTopic = currentScreen.filterTopic,
                    viewModel = viewModel,
                    onNavigateToWord = onNavigateToWord,
                    onNavigateBack = {
                        navigationStack.removeLast()
                    },
                    onStartSavedRevision = onStartSavedRevision
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDashboard(
    viewModel: WordViewModel,
    onNavigateToSubScreen: (LibraryScreenState) -> Unit,
    onNavigateToWord: (Int) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val allWords by viewModel.allWords.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val viewHistory by viewModel.viewHistory.collectAsState()
    val favoriteWordsCount by viewModel.favoriteWordsCount.collectAsState()

    // Derived statistics
    val totalWords = allWords.size
    val totalChapters = allChapters.size
    
    val totalTopics = remember(allWords) {
        allWords.mapNotNull { it.topic }.filter { it.isNotBlank() }.distinct().size
    }
    
    val masteredWords = remember(allWords, viewHistory) {
        val viewedIds = viewHistory.map { it.id }.toSet()
        allWords.count { it.id in viewedIds }
    }
    
    val progressRate = if (totalWords > 0) masteredWords.toFloat() / totalWords else 0f

    // 5 Newest words
    val recentlyAddedWords = remember(allWords) {
        allWords.sortedByDescending { it.dateAdded }.take(5)
    }

    // 5 Recently viewed words (Most Viewed as proxy)
    val recentlyViewedWords = remember(viewHistory) {
        viewHistory.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("library_dashboard_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Statistics Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Knowledge & Reading Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardStat(
                            value = totalWords.toString(),
                            label = "Total Words",
                            color = MaterialTheme.colorScheme.primary
                        )
                        DashboardStat(
                            value = totalChapters.toString(),
                            label = "Chapters",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        DashboardStat(
                            value = totalTopics.toString(),
                            label = "Subjects",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressRate },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(progressRate * 100).toInt()}% View progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Primary Explorer Grid (Chapters, Topics, A-Z)
            Text(
                text = "Library Directories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LibraryExploreCard(
                    title = "Browse Chapters",
                    description = "Browse grouped by source files",
                    icon = Icons.Outlined.Book,
                    bgColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explore_chapters_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.ChapterList) }
                )

                LibraryExploreCard(
                    title = "Browse Subjects",
                    description = "Categorized by subjects",
                    icon = Icons.Outlined.Public,
                    bgColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1565C0),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explore_subjects_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.TopicBrowser) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LibraryExploreCard(
                    title = "A - Z Index",
                    description = "Alphabetical catalog browser",
                    icon = Icons.Outlined.LibraryBooks,
                    bgColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFE65100),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explore_az_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.AlphabeticalIndex) }
                )

                LibraryExploreCard(
                    title = "View All Words",
                    description = "Unfiltered catalog search",
                    icon = Icons.AutoMirrored.Filled.List,
                    bgColor = Color(0xFFEDE7F6),
                    iconColor = Color(0xFF512DA8),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explore_all_words_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.WordList("All Words", WordListType.ALL_WORDS)) }
                )
            }

            // Favorites & Weak Words Row
            Text(
                text = "Focused Study",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudyFocusCard(
                    title = "Saved Favorites",
                    value = favoriteWordsCount.toString(),
                    icon = Icons.Default.Star,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("focus_saved_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.WordList("Starred Favorites", WordListType.FAVORITES)) }
                )

                StudyFocusCard(
                    title = "Word Categories",
                    value = "5 Filtered",
                    icon = Icons.Default.FiberNew,
                    tint = Color(0xFF00B0FF),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("focus_categories_card"),
                    onClick = onNavigateToCategories
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {


                val weakCount = remember(allWords) {
                    allWords.count { it.memoryHook != null && !it.isFavorite }
                        .ifZero { allWords.size / 3 }
                }
                StudyFocusCard(
                    title = "Weak Focus Words",
                    value = weakCount.toString(),
                    icon = Icons.Default.Psychology,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("focus_weak_card"),
                    onClick = { onNavigateToSubScreen(LibraryScreenState.WordList("Needs Focus", WordListType.WEAK_WORDS)) }
                )
            }

            // Recently Added (Horizontal scrolling carousel)
            if (recentlyAddedWords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Added Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToSubScreen(LibraryScreenState.WordList("Recently Added", WordListType.RECENTLY_ADDED)) }
                                .padding(4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recentlyAddedWords.forEach { word ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onNavigateToWord(word.id) }
                                    .testTag("recent_added_item_${word.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FiberNew,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = word.word,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = word.meaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Most Viewed / Recently Viewed List
            if (recentlyViewedWords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Viewed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToSubScreen(LibraryScreenState.WordList("Recently Viewed", WordListType.MOST_VIEWED)) }
                                .padding(4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentlyViewedWords.forEach { word ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onNavigateToWord(word.id) }
                                    .testTag("recent_view_item_${word.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = word.word,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = word.meaning,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStat(
    value: String,
    label: String,
    color: Color
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun LibraryExploreCard(
    title: String,
    description: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StudyFocusCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helpers
private fun Int.ifZero(fallback: () -> Int): Int {
    return if (this == 0) fallback() else this
}
