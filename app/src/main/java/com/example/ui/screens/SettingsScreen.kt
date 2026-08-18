package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preferences.UserPreferences
import com.example.viewmodel.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WordViewModel
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    val totalWords by viewModel.totalWordsCount.collectAsState()
    val favoriteCount by viewModel.favoriteWordsCount.collectAsState()
    val wordsAddedToday by viewModel.wordsAddedTodayCount.collectAsState()
    val googleDocLink by viewModel.googleDocLink.collectAsState()
    val dailyRevisionGoal by viewModel.dailyRevisionGoal.collectAsState()
    val isTimerEnabled by viewModel.isTimerEnabled.collectAsState()

    val syncPreview by viewModel.syncPreview.collectAsState()
    val syncDebugReport by viewModel.syncDebugReport.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var linkInput by remember { mutableStateOf("") }

    val appsScriptUrl by viewModel.appsScriptUrl.collectAsState()
    var appsScriptInput by remember { mutableStateOf("") }
    var showAppsScriptDialog by remember { mutableStateOf(false) }

    var showDeveloperFeatures by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(appsScriptUrl) {
        if (appsScriptInput.isEmpty() && appsScriptUrl.isNotEmpty()) {
            appsScriptInput = appsScriptUrl
        }
    }

    // Synchronize local input field with settings stream
    LaunchedEffect(googleDocLink) {
        if (linkInput.isEmpty() && googleDocLink.isNotEmpty()) {
            linkInput = googleDocLink
        }
    }

    // File picker launcher supporting proper Storage Access Framework
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Failed to persist URI permission", e)
            }
            viewModel.importSpreadsheetFile(it)
        }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Revision Stats", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Revision & Learning Stats",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatLabelValue(label = "Total Dictionary Terms", value = "$totalWords")
                        StatLabelValue(label = "Words Saved", value = "$favoriteCount")
                        StatLabelValue(label = "Learned Today", value = "$wordsAddedToday")
                    }

                    // Circular Progress Bar Simulation/Visual Indicator
                    val progressRatio = remember(totalWords) {
                        if (totalWords == 0) 0f else (favoriteCount.toFloat() / totalWords.toFloat())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Core Retention Rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "${(progressRatio * 100).toInt()}% Saved",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Dark Mode Preferences Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Theme Preference",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            label = "Light",
                            isSelected = themeMode == UserPreferences.ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(UserPreferences.ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            label = "Dark",
                            isSelected = themeMode == UserPreferences.ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(UserPreferences.ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            label = "System",
                            isSelected = themeMode == UserPreferences.ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(UserPreferences.ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Revision Goal Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Daily Revision Goal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        "Target number of words per revision session. Changes apply to new sessions only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            label = "10 Words",
                            isSelected = dailyRevisionGoal == 10,
                            onClick = { viewModel.setDailyRevisionGoal(10) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            label = "20 Words",
                            isSelected = dailyRevisionGoal == 20,
                            onClick = { viewModel.setDailyRevisionGoal(20) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            label = "30 Words",
                            isSelected = dailyRevisionGoal == 30,
                            onClick = { viewModel.setDailyRevisionGoal(30) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Revision Timer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Prompt for a countdown timer before starting a revision session.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTimerEnabled,
                            onCheckedChange = { viewModel.setRevisionTimerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            if (!showDeveloperFeatures) {
                OutlinedButton(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Developer features", fontWeight = FontWeight.SemiBold)
                }
            } else {
            // Document Import & Data backup Actions Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Database & File Import",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Import Button (Using updated array parameters for OpenDocument contract)
                    SettingsActionRow(
                        title = "Import Excel / CSV",
                        description = "Parse and import words from an Excel or CSV file",
                        icon = Icons.Outlined.UploadFile,
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/csv",
                                    "text/comma-separated-values"
                                )
                            )
                        }
                    )

                    HorizontalDivider()

                    // Export Button
                    SettingsActionRow(
                        title = "Backup / Export Database",
                        description = "Share or backup dictionary contents as text format",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val exportStr = viewModel.getDatabaseExportString()
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportStr)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Bodhi Bhasi Dictionary Backup")
                            context.startActivity(shareIntent)
                        }
                    )
                }
            }

            // Google Document Sync Configuration Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Google Sheets Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Synchronize vocabulary words dynamically from a public Google Sheet. Paste either the full URL or a Spreadsheet ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var validationError by remember { mutableStateOf<String?>(null) }

                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { 
                            linkInput = it 
                            validationError = null
                        },
                        label = { Text("Google Sheet Link or ID") },
                        placeholder = { Text("Paste URL or ID here") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = validationError != null,
                        trailingIcon = {
                            if (linkInput.isNotEmpty()) {
                                IconButton(onClick = { linkInput = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear text")
                                }
                            }
                        }
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val trimmed = linkInput.trim()
                                if (trimmed.isEmpty()) {
                                    validationError = "Google Sheet URL/ID cannot be empty."
                                } else {
                                    val isUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://")
                                    val isValid = if (isUrl) {
                                        trimmed.contains("/spreadsheets/d/") || trimmed.contains("/document/d/")
                                    } else {
                                        trimmed.matches("^[a-zA-Z0-9-_]+$".toRegex())
                                    }

                                    if (isValid) {
                                        validationError = null
                                        val docId = com.example.repository.SettingsRepository.extractGoogleDocId(trimmed)
                                        viewModel.setGoogleDocLink(docId)
                                        // Trigger sync right after saving
                                        viewModel.previewGoogleDocSync(docId)
                                    } else {
                                        validationError = "Invalid format. Must be a valid spreadsheet URL or ID containing alphanumeric characters, hyphens, or underscores."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Sync Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Google Sheets Auto-Update (AI Add Word)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Set up a Google Apps Script Web App to automatically append AI-defined words to your Google Sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = appsScriptInput,
                        onValueChange = { 
                            appsScriptInput = it
                            viewModel.setAppsScriptUrl(it)
                        },
                        label = { Text("Apps Script Web App URL") },
                        placeholder = { Text("Paste Apps Script Web App URL here") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (appsScriptInput.isNotEmpty()) {
                                IconButton(onClick = { 
                                    appsScriptInput = ""
                                    viewModel.setAppsScriptUrl("")
                                }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear text")
                                }
                            }
                        }
                    )

                    OutlinedButton(
                        onClick = { showAppsScriptDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("How to set up Apps Script", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            } // End of if (showDeveloperFeatures)

            // Cleanup Actions Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Storage & Maintenance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingsActionRow(
                        title = "Clear Search History",
                        description = "Resets your recent queries listing",
                        icon = Icons.Outlined.History,
                        onClick = { viewModel.clearRecentSearches() }
                    )

                    HorizontalDivider()

                    SettingsActionRow(
                        title = "Reset View History",
                        description = "Clears recently viewed terms logs",
                        icon = Icons.Outlined.CleaningServices,
                        onClick = { viewModel.clearViewHistory() }
                    )
                }
            }

            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Bodhi Bhasi v1.0.0",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Crafted with Material Design 3 guidelines for UPSC aspirants. Beautiful typography, offline persistence, and instant SQLite full text matching.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showPasswordDialog = false 
                    passwordInput = ""
                },
                title = { Text("Developer Access") },
                text = {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Enter Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passwordInput == "\$LexiUPSC") {
                                showDeveloperFeatures = true
                                showPasswordDialog = false
                            }
                            passwordInput = ""
                        }
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showPasswordDialog = false 
                        passwordInput = ""
                    }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Import status feedback alert dialog or toast
        importStatus?.let { status ->
            AlertDialog(
                onDismissRequest = { viewModel.clearImportStatus() },
                title = { Text("Import Status") },
                text = { Text(status) },
                confirmButton = {
                    Button(onClick = { viewModel.clearImportStatus() }) {
                        Text("Awesome")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Active Sync Loading pipeline dialog
        if (isSyncing) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Processing Pipeline...", fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = importStatus ?: "Running synchronization phase...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Phase 1 Downloaded Document Preview Dialog
        syncPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = { viewModel.clearSyncStates() },
                title = { 
                    Text(
                        "Downloaded Document Preview", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "The document was successfully fetched and parsed in-memory. Below is the preview before importing to the local database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Downloaded Document",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                
                                PreviewRow(label = "Chapter", value = preview.chapter)
                                PreviewRow(label = "Topic", value = preview.topic)
                                PreviewRow(label = "Words Detected", value = preview.wordsDetected.toString())
                                PreviewRow(label = "First Word", value = preview.firstWord)
                                PreviewRow(label = "Last Word", value = preview.lastWord)
                            }
                        }
                        
                        Text(
                            "Import this parsed document now?",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmImportAndSync(preview) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("YES")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.clearSyncStates() }) {
                        Text("NO")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Phase 2 Sync Debug Report Dialog
        syncDebugReport?.let { report ->
            AlertDialog(
                onDismissRequest = { viewModel.clearSyncStates() },
                title = { 
                    Text(
                        "Debug Synchronization Report", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Review detailed trace for each pipeline phase below. Total time: ${report.totalTimeMs}ms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Step 1
                        DebugStepCard(
                            stepNumber = 1,
                            title = "Extract Document ID",
                            success = report.step1.success,
                            errorMsg = report.step1.errorMessage
                        ) {
                            DebugMetricRow(label = "Extracted ID", value = report.step1.docId)
                        }

                        // Step-by-Step Step 2
                        DebugStepCard(
                            stepNumber = 2,
                            title = "Construct Export URL",
                            success = report.step2.success,
                            errorMsg = null
                        ) {
                            DebugMetricRow(label = "Export URL", value = report.step2.exportUrl)
                        }

                        // Step-by-Step Step 3
                        DebugStepCard(
                            stepNumber = 3,
                            title = "HTTP Request",
                            success = report.step3.success,
                            errorMsg = report.step3.errorMessage
                        ) {
                            DebugMetricRow(label = "HTTP Code", value = report.step3.httpCode.toString())
                            DebugMetricRow(label = "Content Type", value = report.step3.contentType ?: "unknown")
                            DebugMetricRow(label = "Response Size", value = "${report.step3.responseSize} bytes")
                        }

                        // Step-by-Step Step 4
                        DebugStepCard(
                            stepNumber = 4,
                            title = "Parser",
                            success = report.step4.success,
                            errorMsg = if (report.step4.parserErrors.isNotEmpty()) report.step4.parserErrors.joinToString("; ") else null
                        ) {
                            DebugMetricRow(label = "Number of Lines", value = report.step4.numLines.toString())
                            DebugMetricRow(label = "Words Parsed", value = report.step4.numWordsParsed.toString())
                        }

                        // Step-by-Step Step 5
                        DebugStepCard(
                            stepNumber = 5,
                            title = "Database",
                            success = report.step5.success,
                            errorMsg = report.step5.errorMessage
                        ) {
                            DebugMetricRow(label = "Imported (New)", value = report.step5.imported.toString())
                            DebugMetricRow(label = "Updated (Diff)", value = report.step5.updated.toString())
                            DebugMetricRow(label = "Skipped (Identical)", value = report.step5.skipped.toString())
                            DebugMetricRow(label = "Duplicates Detected", value = report.step5.duplicates.toString())
                            DebugMetricRow(label = "Bookmarks Preserved", value = report.step5.bookmarksPreserved.toString())
                            DebugMetricRow(label = "FTS Indexes Updated", value = report.step5.ftsUpdated.toString())
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearSyncStates() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Debug Report")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (showAppsScriptDialog) {
            AlertDialog(
                onDismissRequest = { showAppsScriptDialog = false },
                title = { Text("Google Apps Script Setup Guide") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "To allow Bodhi Bhasi to add generated words directly to your Google Sheet, follow these steps:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "1. Open your Google Sheet.\n" +
                            "2. Click Extensions > Apps Script.\n" +
                            "3. Replace all default code with the script below.\n" +
                            "4. Click Deploy > New deployment.\n" +
                            "5. Select 'Web app' type.\n" +
                            "6. Set 'Execute as' to 'Me', and 'Who has access' to 'Anyone'.\n" +
                            "7. Deploy, authorize permission, copy the URL and paste it in the field in settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val appsScriptCode = """
                            function doPost(e) {
                              try {
                                var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
                                var data = JSON.parse(e.postData.contents);
                                
                                sheet.appendRow([
                                  data.word || "",
                                  data.meaning || "",
                                  data.example1 || "",
                                  data.example2 || "",
                                  data.example3 || "",
                                  data.memoryHook || "",
                                  data.baseForm || "",
                                  data.subject || "",
                                  data.chapter || "",
                                  data.relatedForms || ""
                                ]);
                                
                                return ContentService.createTextOutput(JSON.stringify({ status: "success" }))
                                  .setMimeType(ContentService.MimeType.JSON);
                              } catch (error) {
                                return ContentService.createTextOutput(JSON.stringify({ status: "error", message: error.toString() }))
                                  .setMimeType(ContentService.MimeType.JSON);
                              }
                            }
                        """.trimIndent()
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Script Code", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = {
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Apps Script", appsScriptCode)
                                        clipboardManager.setPrimaryClip(clip)
                                    }) {
                                        Text("Copy Code")
                                    }
                                }
                                Text(
                                    text = appsScriptCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppsScriptDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DebugStepCard(
    stepNumber: Int,
    title: String,
    success: Boolean,
    errorMsg: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (success) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (success) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $stepNumber: $title",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (success) "✓" else "✗",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            
            content()

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Error: $errorMsg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DebugMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
fun StatLabelValue(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
