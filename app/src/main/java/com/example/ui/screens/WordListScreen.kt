package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
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

enum class WordListType {
    ALL_WORDS,
    RECENTLY_ADDED,
    MOST_VIEWED,
    WEAK_WORDS,
    FAVORITES,
    BY_TOPIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    title: String,
    listType: WordListType,
    filterTopic: String? = null,
    viewModel: WordViewModel,
    onNavigateToWord: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onStartSavedRevision: (() -> Unit)? = null
) {
    val allWords by viewModel.allWords.collectAsState()
    val viewHistory by viewModel.viewHistory.collectAsState()
    val favoriteWords by viewModel.favoriteWords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortOption.ALPHABETICAL) }
    
    // Active filters
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var filterHasMemoryHook by remember { mutableStateOf(false) }
    var filterHasPronunciation by remember { mutableStateOf(false) }
    var filterHasRelatedForms by remember { mutableStateOf(false) }
    var filterHasExamples by remember { mutableStateOf(false) }
    var filterHasOtherForms by remember { mutableStateOf(false) }
    var filterRecentlyViewedOnly by remember { mutableStateOf(false) }

    // Resolve base words based on List Type
    val baseWords = remember(allWords, viewHistory, favoriteWords, listType, filterTopic) {
        when (listType) {
            WordListType.ALL_WORDS -> allWords
            WordListType.RECENTLY_ADDED -> allWords.sortedByDescending { it.dateAdded }
            WordListType.MOST_VIEWED -> {
                // If viewed, they appear in viewHistory
                viewHistory
            }
            WordListType.WEAK_WORDS -> {
                // Return a curated subset of words for focused study (e.g., has memory hook but not yet marked favorite, or simply a subset)
                allWords.filter { it.memoryHook != null && !it.isFavorite }
                    .ifEmpty { allWords.filterIndexed { index, _ -> index % 3 == 0 } }
            }
            WordListType.FAVORITES -> favoriteWords
            WordListType.BY_TOPIC -> {
                if (filterTopic != null) {
                    allWords.filter { it.topic == filterTopic }
                } else {
                    allWords
                }
            }
        }
    }

    // Process filters and sort
    val processedWords = remember(
        baseWords, searchQuery, sortBy, viewHistory,
        filterFavoritesOnly, filterHasMemoryHook, filterHasPronunciation,
        filterHasRelatedForms, filterHasExamples, filterHasOtherForms,
        filterRecentlyViewedOnly
    ) {
        var list = baseWords

        // Local search filter
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.word.contains(searchQuery, ignoreCase = true) ||
                it.meaning.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply filters
        if (filterFavoritesOnly) {
            list = list.filter { it.isFavorite }
        }
        if (filterHasMemoryHook) {
            list = list.filter { !it.memoryHook.isNullOrBlank() }
        }
        if (filterHasPronunciation) {
            list = list.filter { !it.pronunciation.isNullOrBlank() }
        }
        if (filterHasRelatedForms) {
            list = list.filter { !it.relatedForms.isNullOrBlank() }
        }
        if (filterHasExamples) {
            list = list.filter { it.examples.isNotEmpty() }
        }
        if (filterHasOtherForms) {
            list = list.filter { !it.otherForms.isNullOrBlank() }
        }
        if (filterRecentlyViewedOnly) {
            val viewedIds = viewHistory.map { it.id }.toSet()
            list = list.filter { it.id in viewedIds }
        }

        // Apply sort
        when (sortBy) {
            SortOption.ALPHABETICAL -> list.sortedBy { it.word.lowercase() }
            SortOption.NEWEST -> list.sortedByDescending { it.dateAdded }
            SortOption.MOST_VIEWED -> {
                val viewedIndexMap = viewHistory.mapIndexed { idx, w -> w.id to idx }.toMap()
                list.sortedBy { viewedIndexMap[it.id] ?: Int.MAX_VALUE }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("word_list_back_btn")) {
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
            // Search and Sort Row
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
                        .testTag("word_list_search"),
                    placeholder = { Text("Search list...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Sort Dropdown Button
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
                            .testTag("word_list_sort_toggle")
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

            // Custom Filters Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorites filter (hide if already in Favorites list)
                if (listType != WordListType.FAVORITES) {
                    FilterChip(
                        selected = filterFavoritesOnly,
                        onClick = { filterFavoritesOnly = !filterFavoritesOnly },
                        label = { Text("Starred Only") },
                        modifier = Modifier.testTag("chip_favorites")
                    )
                }

                FilterChip(
                    selected = filterHasMemoryHook,
                    onClick = { filterHasMemoryHook = !filterHasMemoryHook },
                    label = { Text("Memory Hooks") },
                    modifier = Modifier.testTag("chip_hooks")
                )

                FilterChip(
                    selected = filterHasExamples,
                    onClick = { filterHasExamples = !filterHasExamples },
                    label = { Text("Has Examples") },
                    modifier = Modifier.testTag("chip_examples")
                )

                FilterChip(
                    selected = filterHasPronunciation,
                    onClick = { filterHasPronunciation = !filterHasPronunciation },
                    label = { Text("Pronunciation") },
                    modifier = Modifier.testTag("chip_pronunciation")
                )

                FilterChip(
                    selected = filterHasRelatedForms,
                    onClick = { filterHasRelatedForms = !filterHasRelatedForms },
                    label = { Text("Related Forms") },
                    modifier = Modifier.testTag("chip_related")
                )

                FilterChip(
                    selected = filterHasOtherForms,
                    onClick = { filterHasOtherForms = !filterHasOtherForms },
                    label = { Text("Other Forms") },
                    modifier = Modifier.testTag("chip_other")
                )

                if (listType != WordListType.MOST_VIEWED) {
                    FilterChip(
                        selected = filterRecentlyViewedOnly,
                        onClick = { filterRecentlyViewedOnly = !filterRecentlyViewedOnly },
                        label = { Text("Recently Viewed") },
                        modifier = Modifier.testTag("chip_viewed")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Listing of Vocabulary Words
            if (processedWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No words found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try clearing search queries or disabling active filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (listType == WordListType.FAVORITES && onStartSavedRevision != null) {
                        item {
                            Button(
                                onClick = { onStartSavedRevision() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Revise Saved Words", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(processedWords, key = { it.id }) { word ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToWord(word.id) }
                                .testTag("list_word_card_${word.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
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
                                        if (word.topic != null) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = word.topic!!,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
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
