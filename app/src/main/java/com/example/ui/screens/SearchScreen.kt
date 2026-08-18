package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.model.MeaningStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.ui.components.HighlightedText
import com.example.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToWord: (Int) -> Unit,
    onNavigateToTopicWords: (String) -> Unit,
    onNavigateToChapterWords: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val generatedWords by viewModel.generatedWords.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val allSubjects by viewModel.allSubjects.collectAsState()
    val aiStatus by viewModel.aiGenerationStatus.collectAsState()
    val selectedMeaningStyle by viewModel.selectedMeaningStyle.collectAsState()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var showAiInputDialog by remember { mutableStateOf(false) }
    var aiWordsList by remember { mutableStateOf(listOf(com.example.model.AiWordRequest(word = "", subject = "", chapter = ""))) }
    var aiSubjectInput by remember { mutableStateOf("") }
    var aiChapterInput by remember { mutableStateOf("") }
    var sameSubjectAndChapter by remember { mutableStateOf(true) }

    // When showing the AI input dialog, pre-fill first word with current query
    LaunchedEffect(showAiInputDialog) {
        if (showAiInputDialog && aiWordsList.size == 1 && aiWordsList[0].word.isBlank()) {
            aiWordsList = listOf(com.example.model.AiWordRequest(word = uiState.query, subject = aiSubjectInput, chapter = aiChapterInput))
        }
    }

    // Automatically focus on the search bar upon screen entry
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Large dynamic search bar
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = {
                            Text(
                                "Search UPSC terms, meanings...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearSearch() },
                                        modifier = Modifier.testTag("search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .focusRequester(focusRequester)
                            .testTag("search_input_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (uiState.query.isNotBlank()) {
                                    viewModel.onSearchExecuted(uiState.query)
                                }
                                keyboardController?.hide()
                            }
                        )
                    )
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.searchScope == com.example.state.SearchScope.WORDS,
                        onClick = { viewModel.updateSearchScope(com.example.state.SearchScope.WORDS) },
                        label = { Text("Words") }
                    )
                    FilterChip(
                        selected = uiState.searchScope == com.example.state.SearchScope.SUBJECT,
                        onClick = { viewModel.updateSearchScope(com.example.state.SearchScope.SUBJECT) },
                        label = { Text("Subject") }
                    )
                    FilterChip(
                        selected = uiState.searchScope == com.example.state.SearchScope.CHAPTER,
                        onClick = { viewModel.updateSearchScope(com.example.state.SearchScope.CHAPTER) },
                        label = { Text("Chapter") }
                    )
                }
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.query.isBlank()) {
                // Recent searches and Suggestions (when query is empty)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Searches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            TextButton(
                                onClick = { viewModel.clearRecentSearches() },
                                modifier = Modifier.testTag("search_clear_history_button")
                            ) {
                                Text(
                                    "Clear All",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentSearches.take(6)) { search ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.updateQuery(search)
                                            viewModel.onSearchExecuted(search)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                        .testTag("recent_search_item_$search"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = search,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteRecentSearch(search) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty Search State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "Ready to Study",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Search across definitions, chapters, and memory hooks optimized for UPSC exams.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Active query states: Loading, Results, or Empty Matches
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("search_loading_indicator")
                        )
                    }
                } else if (uiState.results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (aiStatus != null) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aiStatus ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                if (aiStatus?.startsWith("Error") == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.clearAiStatus() }) {
                                        Text("Retry")
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.SentimentDissatisfied,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    "No matches found for \"${uiState.query}\"",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "We couldn't find any words starting with or containing that query. Check the spelling or search by chapter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        showAiInputDialog = true
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Define with AI & Add to Sheet", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Search Results List with scrolling optimization and keys
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_list")
                    ) {
                        item {
                            Text(
                                text = "Found ${uiState.totalCount} matches",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(
                            items = uiState.results
                        ) { resultItem ->
                            when (resultItem) {
                                is com.example.model.SearchResultItem.WordMatch -> {
                                    val word = resultItem.word
                                    SearchWordItem(
                                        word = word,
                                        query = uiState.query,
                                        onClick = {
                                            viewModel.onSearchExecuted(uiState.query)
                                            keyboardController?.hide()
                                            onNavigateToWord(word.id)
                                        }
                                    )
                                }
                                is com.example.model.SearchResultItem.SubjectMatch -> {
                                    val subject = resultItem.subject
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onSearchExecuted(uiState.query)
                                                keyboardController?.hide()
                                                onNavigateToTopicWords(subject)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(subject, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                                is com.example.model.SearchResultItem.ChapterMatch -> {
                                    val chapter = resultItem.chapter
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onSearchExecuted(uiState.query)
                                                keyboardController?.hide()
                                                onNavigateToChapterWords(chapter)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(chapter, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var subjectExpanded by remember { mutableStateOf(false) }
    var chapterExpanded by remember { mutableStateOf(false) }

    // â”€â”€â”€ AI Input Dialog (Redesigned with scrollable layout) â”€â”€â”€
    if (showAiInputDialog && generatedWords.isEmpty()) {
        AlertDialog(
            onDismissRequest = { showAiInputDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .systemBarsPadding()
                .imePadding(),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Add Words with AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // â”€â”€ Section 1: Meaning Style â”€â”€
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Meaning Detail Level",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeaningStyle.values().forEachIndexed { index, style ->
                                    SegmentedButton(
                                        selected = selectedMeaningStyle == style,
                                        onClick = { viewModel.setMeaningStyle(style) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = MeaningStyle.values().size
                                        )
                                    ) {
                                        Text(
                                            text = style.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selectedMeaningStyle == style) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Text(
                                text = selectedMeaningStyle.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    if (aiStatus?.startsWith("Error") == true) {
                        Text(
                            text = aiStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // â”€â”€ Section 2: Words Input â”€â”€
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Words to Define",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Same for all",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Switch(
                                        checked = sameSubjectAndChapter,
                                        onCheckedChange = { sameSubjectAndChapter = it },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            aiWordsList.forEachIndexed { index, wordReq ->
                                OutlinedTextField(
                                    value = wordReq.word,
                                    onValueChange = { newWord ->
                                        val newList = aiWordsList.toMutableList()
                                        newList[index] = newList[index].copy(word = newWord)
                                        aiWordsList = newList
                                    },
                                    label = { Text("Word ${index + 1}") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        if (aiWordsList.size > 1) {
                                            IconButton(
                                                onClick = { aiWordsList = aiWordsList.toMutableList().also { it.removeAt(index) } },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )
                                
                                if (!sameSubjectAndChapter) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = wordReq.subject,
                                            onValueChange = { newSubj ->
                                                val newList = aiWordsList.toMutableList()
                                                newList[index] = newList[index].copy(subject = newSubj)
                                                aiWordsList = newList
                                            },
                                            label = { Text("Subject") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = wordReq.chapter,
                                            onValueChange = { newChap ->
                                                val newList = aiWordsList.toMutableList()
                                                newList[index] = newList[index].copy(chapter = newChap)
                                                aiWordsList = newList
                                            },
                                            label = { Text("Chapter") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }

                            if (aiWordsList.size < 20) {
                                TextButton(
                                    onClick = { aiWordsList = aiWordsList + com.example.model.AiWordRequest(word = "", subject = if(sameSubjectAndChapter) aiSubjectInput else "", chapter = if(sameSubjectAndChapter) aiChapterInput else "") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Another Word")
                                }
                            }
                        }
                    }

                    // â”€â”€ Section 3: Subject & Chapter (when same for all) â”€â”€
                    if (sameSubjectAndChapter) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Categorization",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val filteredSubjects = allSubjects.filter { it.contains(aiSubjectInput, ignoreCase = true) }
                                
                                OutlinedTextField(
                                    value = aiSubjectInput,
                                    onValueChange = { aiSubjectInput = it; subjectExpanded = true },
                                    label = { Text("Subject") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) subjectExpanded = true }
                                )
                                
                                AnimatedVisibility(visible = subjectExpanded && filteredSubjects.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 150.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        LazyColumn {
                                            items(filteredSubjects.size) { index ->
                                                val selectionOption = filteredSubjects[index]
                                                DropdownMenuItem(
                                                    text = { Text(selectionOption) },
                                                    onClick = {
                                                        aiSubjectInput = selectionOption
                                                        subjectExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                val filteredChapters = allChapters.filter { it.contains(aiChapterInput, ignoreCase = true) }
                                
                                OutlinedTextField(
                                    value = aiChapterInput,
                                    onValueChange = { aiChapterInput = it; chapterExpanded = true },
                                    label = { Text("Chapter") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) chapterExpanded = true }
                                )
                                
                                AnimatedVisibility(visible = chapterExpanded && filteredChapters.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 150.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        LazyColumn {
                                            items(filteredChapters.size) { index ->
                                                val selectionOption = filteredChapters[index]
                                                DropdownMenuItem(
                                                    text = { Text(selectionOption) },
                                                    onClick = {
                                                        aiChapterInput = selectionOption
                                                        chapterExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validWords = aiWordsList.filter { it.word.isNotBlank() }
                        val isInputValid = if (sameSubjectAndChapter) {
                            validWords.isNotEmpty() && aiSubjectInput.isNotBlank() && aiChapterInput.isNotBlank()
                        } else {
                            validWords.isNotEmpty() && validWords.all { it.subject.isNotBlank() && it.chapter.isNotBlank() }
                        }
                        
                        if (isInputValid) {
                            val requests = validWords.map { 
                                if (sameSubjectAndChapter) it.copy(subject = aiSubjectInput, chapter = aiChapterInput) 
                                else it 
                            }
                            viewModel.generateWordsWithAi(requests)
                        }
                    },
                    enabled = aiStatus == null && aiWordsList.any { it.word.isNotBlank() } && 
                        (if (sameSubjectAndChapter) aiSubjectInput.isNotBlank() && aiChapterInput.isNotBlank() 
                         else aiWordsList.filter { it.word.isNotBlank() }.all { it.subject.isNotBlank() && it.chapter.isNotBlank() })
                ) {
                    if (aiStatus != null && !aiStatus!!.startsWith("Error")) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Text("Generate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiInputDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // â”€â”€â”€ Review Generated Words Dialog (Improved layout) â”€â”€â”€
    if (showAiInputDialog && generatedWords.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { 
                showAiInputDialog = false 
                viewModel.clearAiStatus()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .systemBarsPadding()
                .imePadding(),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Review Generated Words",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${generatedWords.size} words ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (aiStatus?.startsWith("Error") == true) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = aiStatus ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    generatedWords.forEachIndexed { index, word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = word.word,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    word.chapter?.let { ch ->
                                        if (ch.isNotBlank()) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                Text(
                                                    text = ch,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = word.meaning,
                                    onValueChange = { newMeaning ->
                                        viewModel.updateGeneratedWordMeaning(index, newMeaning)
                                    },
                                    label = { Text("Meaning") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                if (word.examples.isNotEmpty()) {
                                    Text(
                                        text = "e.g. \"${word.examples.first()}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontStyle = FontStyle.Italic,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveGeneratedWords {
                            showAiInputDialog = false
                            viewModel.clearAiStatus()
                            viewModel.clearSearch()
                            aiWordsList = listOf(com.example.model.AiWordRequest(word = "", subject = "", chapter = ""))
                        }
                    },
                    enabled = aiStatus == null || aiStatus?.startsWith("Error") == true || aiStatus == "Words generated successfully!"
                ) {
                    if (aiStatus?.startsWith("Saving") == true) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Confirm & Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAiInputDialog = false
                        viewModel.clearAiStatus()
                    }
                ) {
                    Text("Discard")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SearchWordItem(
    word: VocabularyWord,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("search_result_item_${word.word}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word Title with matching highlight
                HighlightedText(
                    text = word.word,
                    query = query,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Chapter Badge if exists
                word.chapter?.let { ch ->
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = ch.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Word Meaning with matching highlight
            HighlightedText(
                text = word.meaning,
                query = query,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Example context if exists
            if (word.examples.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        "e.g.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    HighlightedText(
                        text = word.examples.first(),
                        query = query,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontStyle = FontStyle.Italic
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


