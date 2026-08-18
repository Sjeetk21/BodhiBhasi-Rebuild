package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.viewmodel.WordViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlphabeticalIndexScreen(
    viewModel: WordViewModel,
    onNavigateToWord: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allWords by viewModel.allWords.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Group and sort words by first character
    val groupedWords = remember(allWords) {
        allWords.groupBy { it.word.firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }

    // Build the letters that are actually present
    val presentLetters = remember(groupedWords) {
        groupedWords.keys.toList()
    }

    // Full alphabet list for the side-index bar
    val alphabet = ('A'..'Z').toList()

    // Flatten group items to help us map letters to their absolute list position index
    val flatItems = remember(groupedWords) {
        val list = mutableListOf<FlatItem>()
        groupedWords.forEach { (char, words) ->
            list.add(FlatItem.Header(char))
            words.sortedBy { it.word.lowercase() }.forEach { word ->
                list.add(FlatItem.Word(word))
            }
        }
        list
    }

    // Map each present character to its index in the flatItems list
    val letterToIndexMap = remember(flatItems) {
        val map = mutableMapOf<Char, Int>()
        flatItems.forEachIndexed { idx, item ->
            if (item is FlatItem.Header) {
                map[item.char] = idx
            }
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A - Z Index", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("az_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (allWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No words available in the library.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Part: The Sticky-Header word list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    flatItems.forEachIndexed { index, flatItem ->
                        when (flatItem) {
                            is FlatItem.Header -> {
                                stickyHeader(key = "header_${flatItem.char}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = flatItem.char.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            is FlatItem.Word -> {
                                item(key = "word_${flatItem.word.id}") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onNavigateToWord(flatItem.word.id) }
                                            .testTag("az_word_card_${flatItem.word.id}"),
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
                                                    text = flatItem.word.word,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = flatItem.word.meaning,
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

                // Right Part: Premium Sidebar Indexer (Fast A-Z scroll bar)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    alphabet.forEach { letter ->
                        val hasLetter = letterToIndexMap.containsKey(letter)
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (hasLetter) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (hasLetter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = hasLetter) {
                                    val targetIdx = letterToIndexMap[letter]
                                    if (targetIdx != null) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIdx)
                                        }
                                    }
                                }
                                .padding(vertical = 2.dp)
                                .testTag("az_letter_sidebar_$letter")
                        )
                    }
                }
            }
        }
    }
}

sealed class FlatItem {
    data class Header(val char: Char) : FlatItem()
    data class Word(val word: VocabularyWord) : FlatItem()
}
