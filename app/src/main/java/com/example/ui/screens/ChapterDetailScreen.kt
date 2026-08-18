package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    chapterName: String,
    viewModel: WordViewModel,
    onNavigateToWord: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allWords by viewModel.allWords.collectAsState()
    val viewHistory by viewModel.viewHistory.collectAsState()

    val chapterWords = remember(allWords, chapterName) {
        allWords.filter { it.chapter == chapterName }
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortOption.ALPHABETICAL) } // Sorting option
    var showOnlyFavorites by remember { mutableStateOf(false) } // Favorite filter

    // Calculate Chapter Statistics
    val stats = remember(chapterWords, viewHistory) {
        val total = chapterWords.size
        val viewedIds = viewHistory.map { it.id }.toSet()
        val mastered = chapterWords.count { it.id in viewedIds }
        val favorites = chapterWords.count { it.isFavorite }
        val progress = if (total > 0) mastered.toFloat() / total else 0f
        ChapterStats(total, mastered, favorites, progress)
    }

    // Filtered and sorted words
    val filteredSortedWords = remember(chapterWords, searchQuery, sortBy, showOnlyFavorites) {
        var list = chapterWords
        
        // Apply local search query
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.word.contains(searchQuery, ignoreCase = true) ||
                it.meaning.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Apply favorite filter
        if (showOnlyFavorites) {
            list = list.filter { it.isFavorite }
        }
        
        // Apply sort
        when (sortBy) {
            SortOption.ALPHABETICAL -> list.sortedBy { it.word.lowercase() }
            SortOption.NEWEST -> list.sortedByDescending { it.dateAdded }
            SortOption.MOST_VIEWED -> {
                // Approximate viewed order by presence in viewHistory
                val viewedIndexMap = viewHistory.mapIndexed { idx, w -> w.id to idx }.toMap()
                list.sortedBy { viewedIndexMap[it.id] ?: Int.MAX_VALUE }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapterName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("chapter_detail_back_btn")) {
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
        ) {
            // Elegant Premium Statistics Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatItem(
                            title = "Total Terms",
                            value = stats.total.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            title = "Mastered",
                            value = "${stats.mastered}/${stats.total}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatItem(
                            title = "Saved Favorites",
                            value = stats.favorites.toString(),
                            color = Color(0xFFE91E63) // Custom warm pink
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { stats.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(stats.progress * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interactive Search & Filter Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chapter_detail_search"),
                    placeholder = { Text("Search inside chapter...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Favorite filter toggle
                IconButton(
                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (showOnlyFavorites) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("chapter_detail_fav_toggle")
                ) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorites",
                        tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sorting dialog toggle
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("chapter_detail_sort_toggle")
                    ) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Sort Options")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alphabetical A-Z") },
                            onClick = {
                                sortBy = SortOption.ALPHABETICAL
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Newest Added") },
                            onClick = {
                                sortBy = SortOption.NEWEST
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.FormatListNumbered, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Viewed") },
                            onClick = {
                                sortBy = SortOption.MOST_VIEWED
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                        )
                    }
                }
            }

            // Word Listing View
            if (filteredSortedWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No words match your filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try modifying your search or favorite filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSortedWords) { word ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToWord(word.id) }
                                .testTag("chapter_word_card_${word.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = word.word,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (word.isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Starred",
                                                tint = Color(0xFFE91E63),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
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

enum class SortOption {
    ALPHABETICAL,
    NEWEST,
    MOST_VIEWED
}

data class ChapterStats(
    val total: Int,
    val mastered: Int,
    val favorites: Int,
    val progress: Float
)

@Composable
fun StatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}
