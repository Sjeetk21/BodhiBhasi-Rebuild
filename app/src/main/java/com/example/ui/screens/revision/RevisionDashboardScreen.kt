package com.example.ui.screens.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.viewmodel.RevisionDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionDashboardScreen(
    viewModel: RevisionDashboardViewModel,
    onStartSession: (Int?) -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTimerEnabled by viewModel.isTimerEnabled.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Revision Engine", fontWeight = FontWeight.ExtraBold) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Main Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Today's Revision",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            IconButton(
                                onClick = onNavigateToAnalytics,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Learning Analytics",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.wordsDueToday}",
                                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Words Due",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            CircularProgressIndicator(
                                progress = { if (uiState.wordsDueToday == 0) 1f else 0f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 8.dp,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }

                // Sub Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Revised", style = MaterialTheme.typography.labelSmall)
                            Text("${uiState.wordsRevisedToday}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Streak", style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("${uiState.revisionStreak} d", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Learned", style = MaterialTheme.typography.labelSmall)
                            Text("${uiState.wordsLearned}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Text(
                    text = "Estimated Time: ~${uiState.estimatedSessionTime} mins",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Take available space
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            Text(
                                "Error Report (Screenshot this)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Start Button
                Button(
                    onClick = {
                        if (isTimerEnabled) {
                            showTimerDialog = true
                        } else {
                            onStartSession(null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.wordsDueToday > 0) "Quick Start" else "Practice Anyway",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showTimerDialog) {
            AlertDialog(
                onDismissRequest = { showTimerDialog = false },
                title = { Text("Revision Timer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Choose a duration for this revision session.")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { onStartSession(5); showTimerDialog = false }, modifier = Modifier.weight(1f)) {
                                Text("5 m")
                            }
                            OutlinedButton(onClick = { onStartSession(10); showTimerDialog = false }, modifier = Modifier.weight(1f)) {
                                Text("10 m")
                            }
                            OutlinedButton(onClick = { onStartSession(15); showTimerDialog = false }, modifier = Modifier.weight(1f)) {
                                Text("15 m")
                            }
                        }
                        
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = { customMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Custom Duration (min)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val minutes = customMinutesInput.toIntOrNull()
                        if (minutes != null && minutes > 0) {
                            onStartSession(minutes)
                        } else {
                            onStartSession(null)
                        }
                        showTimerDialog = false
                    }) {
                        Text("Start")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimerDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
