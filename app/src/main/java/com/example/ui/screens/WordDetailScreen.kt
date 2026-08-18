package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.viewmodel.WordViewModel
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    wordId: Int,
    listContext: String = "All",
    viewModel: WordViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allWords by viewModel.allWords.collectAsState()
    val viewHistory by viewModel.viewHistory.collectAsState()
    val favoriteWords by viewModel.favoriteWords.collectAsState()

    val pagerWords = remember(allWords, listContext, viewHistory, favoriteWords) {
        when {
            listContext == "Saved" -> favoriteWords
            listContext == "History" -> viewHistory.distinctBy { it.id }
            listContext == "Alphabetical" -> allWords.sortedBy { it.word.lowercase() }
            listContext.startsWith("Chapter:") -> {
                val ch = listContext.removePrefix("Chapter:")
                allWords.filter { it.chapter == ch }
            }
            listContext.startsWith("Topic:") -> {
                val tp = listContext.removePrefix("Topic:")
                allWords.filter { it.topic == tp }
            }
            else -> allWords
        }
    }

    val initialIndex = remember(pagerWords, wordId) {
        val idx = pagerWords.indexOfFirst { it.id == wordId }
        if (idx == -1) 0 else idx
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) {
        if (pagerWords.isEmpty()) 1 else pagerWords.size
    }

    LaunchedEffect(pagerState.settledPage, pagerWords) {
        if (pagerWords.isNotEmpty() && pagerState.settledPage < pagerWords.size) {
            if (listContext != "History") {
                viewModel.viewWord(pagerWords[pagerState.settledPage].id)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize TextToSpeech engine dynamically for live audio pronunciation
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        ttsInstance.language = Locale.US
        tts = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Interactive expansion states for cards
    var meaningExpanded by remember { mutableStateOf(true) }
    var examplesExpanded by remember { mutableStateOf(true) }
    var formsExpanded by remember { mutableStateOf(false) } // Autocollapsed
    var hookExpanded by remember { mutableStateOf(true) }

    val currentWord = if (pagerWords.isNotEmpty() && pagerState.currentPage < pagerWords.size) {
        pagerWords[pagerState.currentPage]
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Oxford Dictionary View", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    currentWord?.let { w ->
                        // Share option in top bar as secondary option
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "LexiUPSC Word: ${w.word}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "LexiUPSC Vocab: ${w.word}\n\n" +
                                            "Meaning: ${w.meaning}\n\n" +
                                            if (w.examples.isNotEmpty()) "Example: ${w.examples.first()}" else ""
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Word"))
                        }) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share Word")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (pagerWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Word details not found.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val word = pagerWords[page]
                Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Oxford Hero Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Subject and Chapter badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            word.chapter?.let { ch ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        ch.uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }

                            word.topic?.let { tp ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        tp.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Word Name — auto-size for long words
                        val wordFontSize = when {
                            word.word.length > 14 -> 28.sp
                            word.word.length > 10 -> 34.sp
                            else -> 40.sp
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp,
                                    fontSize = wordFontSize
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                modifier = Modifier.testTag("detail_word_title")
                            )
                            if (isTtsReady) {
                                IconButton(
                                    onClick = { tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, "pronounce_id") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Pronounce",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        if (!word.pronunciation.isNullOrEmpty()) {
                            Text(
                                text = "(${word.pronunciation})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bookmark button
                            Button(
                                onClick = { viewModel.toggleFavorite(word.id) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("detail_bookmark_toggle")
                            ) {
                                Icon(
                                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (word.isFavorite) Color.Red else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (word.isFavorite) "Saved" else "Save",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Copy Word button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("LexiUPSC Word", "${word.word}: ${word.meaning}")
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Word copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                    .testTag("detail_copy_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy Word",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Collapsible Meaning Block
                InteractiveDetailSection(
                    title = "Definition & Meaning",
                    isExpanded = meaningExpanded,
                    onToggle = { meaningExpanded = !meaningExpanded },
                    testTag = "detail_section_meaning"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Meaning of ${word.word}", word.meaning)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Definition copied!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Definition", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Collapsible Examples Block (Hide if empty)
                if (word.examples.isNotEmpty()) {
                    InteractiveDetailSection(
                        title = "Examples & Usage Context",
                        isExpanded = examplesExpanded,
                        onToggle = { examplesExpanded = !examplesExpanded },
                        testTag = "detail_section_examples"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            word.examples.forEachIndexed { index, example ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = example,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontStyle = FontStyle.Italic
                                            ),
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Collapsible Forms and Inflections Block (Autocollapsed, Hide if empty)
                val hasForms = !word.baseForm.isNullOrEmpty() || !word.otherForms.isNullOrEmpty() || !word.relatedForms.isNullOrEmpty()
                if (hasForms) {
                    InteractiveDetailSection(
                        title = "Forms & Inflections",
                        isExpanded = formsExpanded,
                        onToggle = { formsExpanded = !formsExpanded },
                        testTag = "detail_section_forms"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!word.baseForm.isNullOrEmpty()) {
                                DetailFormRow(label = "Base Form", value = word.baseForm)
                            }
                            if (!word.otherForms.isNullOrEmpty()) {
                                DetailFormRow(label = "Other Forms", value = word.otherForms)
                            }
                            if (!word.relatedForms.isNullOrEmpty()) {
                                DetailFormRow(label = "Related Forms", value = word.relatedForms)
                            }
                        }
                    }
                }

                // Antonyms Block (Hide if empty)
                if (!word.antonyms.isNullOrEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Flip, // or any antonym icon like CompareArrows
                                    contentDescription = "Antonyms",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Antonyms",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = word.antonyms,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Collapsible Memory Hook Block (Hide if empty)
                if (!word.memoryHook.isNullOrEmpty()) {
                    InteractiveDetailSection(
                        title = "Study Mnemonics & Memory Hook",
                        isExpanded = hookExpanded,
                        onToggle = { hookExpanded = !hookExpanded },
                        testTag = "detail_section_mnemonics"
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Memory Hook",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Memory Strategy:",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                                Text(
                                    text = word.memoryHook,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Metadata details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Study Record Added:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            DateFormat.getDateInstance(DateFormat.LONG).format(Date(word.dateAdded)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Safe spacer at bottom
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }
}

@Composable
fun InteractiveDetailSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ArrowRotate"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(animationSpec = tween(300))
        ) {
            // Header Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .rotate(rotationState)
                        .size(24.dp)
                )
            }

            // Expanded content section
            if (isExpanded) {
                Box(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun DetailFormRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
