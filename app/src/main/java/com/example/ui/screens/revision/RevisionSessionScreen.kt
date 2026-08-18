package com.example.ui.screens.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LocalAnswerEvaluator
import com.example.viewmodel.RevisionSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionSessionScreen(
    viewModel: RevisionSessionViewModel,
    onlySavedWords: Boolean = false,
    durationMinutes: Int? = null,
    onSessionFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isFinished) {
        LaunchedEffect(Unit) { onSessionFinished() }
        return
    }

    LaunchedEffect(onlySavedWords) {
        viewModel.startSession(onlySavedWords)
    }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    var remainingSeconds by remember(durationMinutes) { 
        mutableStateOf(durationMinutes?.times(60)) 
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds != null && remainingSeconds!! > 0 && !uiState.isFinished) {
            kotlinx.coroutines.delay(1000L)
            remainingSeconds = remainingSeconds!! - 1
            if (remainingSeconds == 0) {
                viewModel.forceEndSession()
            }
        }
    }

    // --- Bonus Round Prompt Dialog ---
    if (uiState.showBonusPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.declineBonusRound() },
            confirmButton = {
                Button(onClick = { viewModel.startBonusRound() }) {
                    Text("Yes, Review Them", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.declineBonusRound() }) {
                    Text("No, I'm Done")
                }
            },
            title = {
                Text("Bonus Round?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You struggled with a few words today. Would you like to do a quick extra review of those specific words?",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        )
        // Don't return here — let the scaffold render underneath the dialog
    }

    if (uiState.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "You're all caught up!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "There are no words due for revision right now. Great job!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onSessionFinished() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Return to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayNumber = minOf(uiState.wordsRevised + 1, uiState.wordsTotal)
                            Text(
                                "$displayNumber / ${uiState.wordsTotal}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            if (remainingSeconds != null) {
                                val mins = remainingSeconds!! / 60
                                val secs = remainingSeconds!! % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingSeconds!! < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LinearProgressIndicator(
                    progress = {
                        if (uiState.wordsTotal == 0) 0f
                        else (uiState.wordsRevised.toFloat() / uiState.wordsTotal).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val word = uiState.currentWord
            if (word != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Word Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce Word")
                            }
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (word.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!word.pronunciation.isNullOrEmpty()) {
                            Text(
                                text = "(${word.pronunciation})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!uiState.isRevealed) {
                        if (uiState.isModeA) {
                            ActiveRecallInput(
                                onSubmit = { viewModel.submitAnswer(it) },
                                onDontKnow = { viewModel.submitAnswer("") }
                            )
                        } else {
                            val options = remember(word.word) {
                                (uiState.distractors + word.meaning).shuffled()
                            }
                            MultipleChoiceInput(
                                options = options,
                                onSelect = { viewModel.submitModeBAnswer(it) }
                            )
                        }
                    } else {
                        RevealedAnswerView(
                            word = word,
                            evaluationResult = uiState.evaluationResult,
                            onSelfEvaluate = { viewModel.evaluateSelf(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRecallInput(onSubmit: (String) -> Unit, onDontKnow: () -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type the meaning...") },
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )
        Button(
            onClick = { onSubmit(text) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = text.isNotBlank()
        ) {
            Text("Check Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { onDontKnow() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("I Don't Know", fontSize = 14.sp)
        }
    }
}

@Composable
fun MultipleChoiceInput(options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            OutlinedButton(
                onClick = { onSelect(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = option,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RevealedAnswerView(
    word: com.example.model.VocabularyWord,
    evaluationResult: LocalAnswerEvaluator.EvaluationLevel?,
    onSelfEvaluate: (com.example.engine.LexiRevisionEngine.Feedback) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {

        if (evaluationResult != null) {
            val (color, icon, text) = when (evaluationResult) {
                LocalAnswerEvaluator.EvaluationLevel.CORRECT -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.Check, "Correct")
                LocalAnswerEvaluator.EvaluationLevel.PARTIALLY_CORRECT -> Triple(MaterialTheme.colorScheme.tertiary, Icons.Default.Check, "Partially Correct")
                LocalAnswerEvaluator.EvaluationLevel.INCORRECT -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Close, "Incorrect")
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = color)
                Text(text, color = color, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Meaning", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!word.memoryHook.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Memory Hook", style = MaterialTheme.typography.labelSmall)
                    Text(word.memoryHook!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                if (word.examples.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Examples", style = MaterialTheme.typography.labelSmall)
                    word.examples.forEach { example ->
                        Text("\"$example\"", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }

                if (!word.baseForm.isNullOrBlank() || !word.relatedForms.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    if (!word.baseForm.isNullOrBlank()) {
                        Text("Base Form: ${word.baseForm}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!word.relatedForms.isNullOrBlank()) {
                        Text("Related: ${word.relatedForms}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (!word.antonyms.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Antonyms", style = MaterialTheme.typography.labelSmall)
                    Text(word.antonyms!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Text("How well did you remember?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onSelfEvaluate(com.example.engine.LexiRevisionEngine.Feedback.I_DONT_KNOW) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha=0.6f), contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) { Text("Don't Know", textAlign = TextAlign.Center) }

            Button(
                onClick = { onSelfEvaluate(com.example.engine.LexiRevisionEngine.Feedback.NEED_REVISION) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) { Text("Need Revision", textAlign = TextAlign.Center) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onSelfEvaluate(com.example.engine.LexiRevisionEngine.Feedback.ALMOST) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) { Text("Almost", textAlign = TextAlign.Center) }

            Button(
                onClick = { onSelfEvaluate(com.example.engine.LexiRevisionEngine.Feedback.GOT_IT) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) { Text("Got It", textAlign = TextAlign.Center) }
        }
    }
}
