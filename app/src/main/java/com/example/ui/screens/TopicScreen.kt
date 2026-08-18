package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    viewModel: WordViewModel,
    onNavigateToTopicWords: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allWords by viewModel.allWords.collectAsState()
    val viewHistory by viewModel.viewHistory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Dynamically calculate topics and word counts
    val topicsList = remember(allWords) {
        allWords.mapNotNull { it.topic }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredTopics = remember(searchQuery, topicsList) {
        if (searchQuery.isBlank()) {
            topicsList
        } else {
            topicsList.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse by Subjects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("topics_back_btn")) {
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
            // Elegant Topic Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("topics_search_input"),
                placeholder = { Text("Search subjects...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (filteredTopics.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No subjects found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredTopics) { topic ->
                        val topicWords = remember(allWords, topic) {
                            allWords.filter { it.topic == topic }
                        }
                        val count = topicWords.size
                        
                        // Calculate topic study progress
                        val viewedCount = remember(topicWords, viewHistory) {
                            val viewedIds = viewHistory.map { it.id }.toSet()
                            topicWords.count { it.id in viewedIds }
                        }
                        val progress = if (count > 0) viewedCount.toFloat() / count else 0f

                        val iconInfo = getTopicIcon(topic)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onNavigateToTopicWords(topic) }
                                .testTag("topic_card_$topic"),
                            colors = CardDefaults.cardColors(
                                containerColor = iconInfo.bgColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(iconInfo.bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconInfo.icon,
                                            contentDescription = null,
                                            tint = iconInfo.tintColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }

                                Column {
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count terms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = iconInfo.tintColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = iconInfo.tintColor
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
}

data class TopicIconInfo(
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color
)

fun getTopicIcon(topic: String): TopicIconInfo {
    val lower = topic.lowercase()
    return when {
        lower.contains("polity") || lower.contains("constitution") || lower.contains("law") -> {
            TopicIconInfo(
                icon = Icons.Outlined.Gavel,
                bgColor = Color(0xFFFFEBEE),
                tintColor = Color(0xFFC62828)
            )
        }
        lower.contains("economy") || lower.contains("economic") || lower.contains("budget") || lower.contains("finance") -> {
            TopicIconInfo(
                icon = Icons.Outlined.TrendingUp,
                bgColor = Color(0xFFE8F5E9),
                tintColor = Color(0xFF2E7D32)
            )
        }
        lower.contains("history") || lower.contains("art") || lower.contains("culture") || lower.contains("ancient") -> {
            TopicIconInfo(
                icon = Icons.Outlined.AccountBalance,
                bgColor = Color(0xFFFFF3E0),
                tintColor = Color(0xFFEF6C00)
            )
        }
        lower.contains("geography") || lower.contains("environment") || lower.contains("disaster") || lower.contains("map") -> {
            TopicIconInfo(
                icon = Icons.Outlined.Public,
                bgColor = Color(0xFFE0F7FA),
                tintColor = Color(0xFF00838F)
            )
        }
        lower.contains("science") || lower.contains("tech") || lower.contains("biotech") || lower.contains("space") || lower.contains("computer") -> {
            TopicIconInfo(
                icon = Icons.Outlined.Science,
                bgColor = Color(0xFFEDE7F6),
                tintColor = Color(0xFF4527A0)
            )
        }
        lower.contains("international") || lower.contains("relation") || lower.contains("foreign") || lower.contains("ir") -> {
            TopicIconInfo(
                icon = Icons.Outlined.Language,
                bgColor = Color(0xFFE1F5FE),
                tintColor = Color(0xFF0277BD)
            )
        }
        lower.contains("ethics") || lower.contains("integrity") || lower.contains("aptitude") -> {
            TopicIconInfo(
                icon = Icons.Outlined.Psychology,
                bgColor = Color(0xFFF1F8E9),
                tintColor = Color(0xFF558B2F)
            )
        }
        else -> {
            TopicIconInfo(
                icon = Icons.Outlined.Book,
                bgColor = Color(0xFFECEFF1),
                tintColor = Color(0xFF37474F)
            )
        }
    }
}
