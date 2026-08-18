package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningAnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Light, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Typography-Driven Hero Section
                HeroStatsSection(uiState)

                // Animated Donut Chart for Learning Distribution
                PremiumLearningDistribution(
                    distribution = uiState.learningDistribution,
                    total = uiState.totalWordsTracked.coerceAtLeast(1)
                )

                // Animated 30-Day Activity Chart with Gradients
                PremiumActivityChart(data = uiState.dailyRevisionsHistory)
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun HeroStatsSection(uiState: com.example.viewmodel.AnalyticsUiState) {
    val retentionRate = if (uiState.totalWordsTracked > 0) {
        (uiState.totalWordsMastered.toFloat() / uiState.totalWordsTracked * 100).toInt()
    } else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroStat("Words Tracked", "${uiState.totalWordsTracked}", modifier = Modifier.weight(1f))
        
        Divider(
            modifier = Modifier.height(40.dp).width(1.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        HeroStat("Mastered", "${uiState.totalWordsMastered}", modifier = Modifier.weight(1f))
        
        Divider(
            modifier = Modifier.height(40.dp).width(1.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        HeroStat("Retention", "$retentionRate%", modifier = Modifier.weight(1f))
    }
}

@Composable
fun HeroStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Light,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PremiumLearningDistribution(distribution: Map<String, Int>, total: Int) {
    val newCount = distribution["NEW"] ?: 0
    val learningCount = distribution["LEARNING"] ?: 0
    val familiarCount = distribution["FAMILIAR"] ?: 0
    val masteredCount = distribution["MASTERED"] ?: 0

    val newColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val learningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
    val familiarColor = MaterialTheme.colorScheme.secondary
    val masteredColor = MaterialTheme.colorScheme.primary

    // Animation state
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, delayMillis = 100)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Learning Journey",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeW = 16.dp.toPx()
                val radius = (size.minDimension - strokeW) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                var currentStartAngle = -90f
                val totalSweep = 360f * animProgress.value

                // Draw Track (Empty State)
                drawCircle(
                    color = newColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeW)
                )

                if (total > 0 && totalSweep > 0) {
                    // Draw Mastered
                    val masteredSweep = (masteredCount.toFloat() / total) * totalSweep
                    if (masteredSweep > 0) {
                        drawArc(
                            color = masteredColor,
                            startAngle = currentStartAngle,
                            sweepAngle = masteredSweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        currentStartAngle += masteredSweep
                    }

                    // Draw Familiar
                    val familiarSweep = (familiarCount.toFloat() / total) * totalSweep
                    if (familiarSweep > 0) {
                        drawArc(
                            color = familiarColor,
                            startAngle = currentStartAngle,
                            sweepAngle = familiarSweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        currentStartAngle += familiarSweep
                    }

                    // Draw Learning
                    val learningSweep = (learningCount.toFloat() / total) * totalSweep
                    if (learningSweep > 0) {
                        drawArc(
                            color = learningColor,
                            startAngle = currentStartAngle,
                            sweepAngle = learningSweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        currentStartAngle += learningSweep
                    }
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light)
                )
                Text(
                    text = "Words",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Premium Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Mastered", masteredCount, masteredColor)
            LegendItem("Familiar", familiarCount, familiarColor)
            LegendItem("Learning", learningCount, learningColor)
            LegendItem("New", newCount, newColor)
        }
    }
}

@Composable
fun PremiumActivityChart(data: List<Int>) {
    val chronologicalData = data.reversed() // So index 0 is oldest, last is today
    val maxVal = chronologicalData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val numDays = data.size
    
    val topColor = MaterialTheme.colorScheme.primary
    val bottomColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val gridLineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)

    // Animation state
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, delayMillis = 300)
        )
    }

    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val labels = List(numDays) { i ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(numDays - 1 - i))
        dateFormat.format(cal.time)
    }
    
    val scrollState = rememberScrollState()
    
    // Auto-scroll to the end (latest date)
    LaunchedEffect(chronologicalData) {
        scrollState.animateScrollTo(scrollState.maxValue, animationSpec = tween(500))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Activity (Last 30 Days)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(40.dp)) // Space for floating numbers
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            val itemWidth = 60.dp
            
            Box(
                modifier = Modifier
                    .width(itemWidth * numDays)
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasHeight = size.height
                    
                    // Draw faint grid lines spanning entire width
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = canvasHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    val brush = Brush.verticalGradient(
                        colors = listOf(topColor, bottomColor)
                    )

                    val barWidthPx = 16.dp.toPx()
                    val spacingPx = itemWidth.toPx()
                    
                    for (i in 0 until numDays) {
                        val value = chronologicalData[i]
                        val heightRatio = (value.toFloat() / maxVal) * animProgress.value
                        val barHeight = canvasHeight * heightRatio
                        
                        val x = (i * spacingPx) + (spacingPx - barWidthPx) / 2
                        val y = canvasHeight - barHeight
                        
                        if (value > 0) {
                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(x, y),
                                size = Size(barWidthPx, barHeight),
                                cornerRadius = CornerRadius(barWidthPx / 2, barWidthPx / 2)
                            )
                        } else {
                            // Elegant tiny dash for zero values
                            drawRoundRect(
                                color = gridLineColor,
                                topLeft = Offset(x, canvasHeight - 2.dp.toPx()),
                                size = Size(barWidthPx, 2.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState, enabled = false) // Synchronized scroll with the chart above
        ) {
            val itemWidth = 60.dp
            
            labels.forEachIndexed { index, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(itemWidth)
                ) {
                    val value = chronologicalData[index]
                    Text(
                        text = if (value > 0) "$value" else "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
        Text(text = "$count", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}
