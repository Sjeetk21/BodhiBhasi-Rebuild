package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AnalyticsUiState
import com.example.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Vibrant Theme Color Palette for Analytics
private val AnalyticsCyan = Color(0xFF38BDF8)
private val AnalyticsPurple = Color(0xFFA78BFA)
private val AnalyticsOrange = Color(0xFFFB923C)
private val AnalyticsGreen = Color(0xFF4ADE80)
private val AnalyticsPink = Color(0xFFF472B6)
private val AnalyticsYellow = Color(0xFFFACC15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningAnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics & Insights",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                CircularProgressIndicator(strokeWidth = 3.dp, color = AnalyticsCyan)
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                
                // 1. Hero Stats Row (Words, Mastered, Retention %, Streak 🔥)
                HeroStatsSection(uiState)

                // 2. Weekly Snapshot Card
                WeeklySnapshotCard(uiState)

                // 3. Learning Journey Donut Chart
                PremiumLearningDistribution(
                    distribution = uiState.learningDistribution,
                    total = uiState.totalWordsTracked.coerceAtLeast(1)
                )

                // 4. Subject Breakdown Horizontal Bars
                if (uiState.subjectDistribution.isNotEmpty()) {
                    SubjectBreakdownSection(uiState.subjectDistribution)
                }

                // 5. Activity (Last 30 Days Bar Chart)
                PremiumActivityChart(data = uiState.dailyRevisionsHistory)

                // 6. At a Glance Quick Stats Grid
                QuickStatsGrid(uiState)
                
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ─── Hero Stats Section ──────────────────────────────────────────────

@Composable
fun HeroStatsSection(uiState: AnalyticsUiState) {
    val retentionRate = if (uiState.totalWordsTracked > 0) {
        (uiState.totalWordsMastered.toFloat() / uiState.totalWordsTracked * 100).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    AnalyticsYellow.copy(alpha = 0.3f),
                    AnalyticsCyan.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroStatItem(
                title = "Words",
                value = "${uiState.totalWordsTracked}",
                color = AnalyticsYellow,
                modifier = Modifier.weight(1f)
            )
            VerticalDividerThin()
            HeroStatItem(
                title = "Mastered",
                value = "${uiState.totalWordsMastered}",
                color = AnalyticsGreen,
                modifier = Modifier.weight(1f)
            )
            VerticalDividerThin()
            HeroStatItem(
                title = "Retention",
                value = "$retentionRate%",
                color = AnalyticsCyan,
                modifier = Modifier.weight(1f)
            )
            VerticalDividerThin()
            HeroStatItem(
                title = "Streak",
                value = if (uiState.learningStreak > 0) "${uiState.learningStreak}🔥" else "0",
                color = AnalyticsOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VerticalDividerThin() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}

@Composable
fun HeroStatItem(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// ─── Weekly Snapshot Card ────────────────────────────────────

@Composable
fun WeeklySnapshotCard(uiState: AnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AnalyticsCyan.copy(alpha = 0.08f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    AnalyticsCyan.copy(alpha = 0.35f),
                    AnalyticsPurple.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = AnalyticsCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Weekly Snapshot",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SnapshotMetric(
                    icon = Icons.Outlined.AddCircle,
                    value = "${uiState.wordsAddedThisWeek}",
                    label = "Added",
                    color = AnalyticsCyan,
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    icon = Icons.Outlined.TrendingUp,
                    value = String.format(Locale.US, "%.1f", uiState.averageDailyRevisions),
                    label = "Avg/Day",
                    color = AnalyticsGreen,
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    icon = Icons.Outlined.EmojiEvents,
                    value = "${uiState.bestDay}",
                    label = "Best Day",
                    color = AnalyticsYellow,
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    icon = Icons.Outlined.Warning,
                    value = "${uiState.difficultWords}",
                    label = "Challenging",
                    color = AnalyticsOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SnapshotMetric(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

// ─── Subject Breakdown ───────────────────────────────────────

@Composable
fun SubjectBreakdownSection(subjects: List<Pair<String, Int>>) {
    val maxCount = subjects.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(1000, delayMillis = 200))
    }

    val subjectColors = listOf(
        AnalyticsPurple,
        AnalyticsCyan,
        AnalyticsGreen,
        AnalyticsOrange,
        AnalyticsPink,
        AnalyticsYellow
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = AnalyticsPurple,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Subject Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                subjects.forEachIndexed { index, (topic, count) ->
                    val color = subjectColors[index % subjectColors.size]
                    SubjectBarRow(
                        topic = topic,
                        count = count,
                        maxCount = maxCount,
                        progress = animProgress.value,
                        barColor = color
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectBarRow(
    topic: String,
    count: Int,
    maxCount: Int,
    progress: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = topic,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count words",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = barColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = ((count.toFloat() / maxCount) * progress).coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.5f),
                                barColor
                            )
                        )
                    )
            )
        }
    }
}

// ─── Quick Stats Grid ────────────────────────────────────────

@Composable
fun QuickStatsGrid(uiState: AnalyticsUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = null,
                tint = AnalyticsYellow,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "At a Glance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                icon = Icons.Outlined.Bookmark,
                value = "${uiState.totalBookmarks}",
                label = "Bookmarks",
                color = AnalyticsYellow,
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                icon = Icons.Outlined.CalendarMonth,
                value = "${uiState.wordsAddedThisMonth}",
                label = "This Month",
                color = AnalyticsCyan,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                icon = Icons.Outlined.Speed,
                value = String.format(Locale.US, "%.1f", uiState.averageDifficulty),
                label = "Avg Difficulty",
                color = AnalyticsOrange,
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                icon = Icons.Outlined.Psychology,
                value = String.format(Locale.US, "%.1f", uiState.averageStability),
                label = "Avg Stability",
                color = AnalyticsGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Donut Chart Section ─────────────────────────────────────

@Composable
fun PremiumLearningDistribution(distribution: Map<String, Int>, total: Int) {
    val newCount = distribution["NEW"] ?: 0
    val learningCount = distribution["LEARNING"] ?: 0
    val familiarCount = distribution["FAMILIAR"] ?: 0
    val masteredCount = distribution["MASTERED"] ?: 0

    val newColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val learningColor = AnalyticsPink
    val familiarColor = AnalyticsCyan
    val masteredColor = AnalyticsGreen

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1200, delayMillis = 100))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PieChart,
                    contentDescription = null,
                    tint = AnalyticsGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Learning Journey",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeW = 14.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    var currentStartAngle = -90f
                    val totalSweep = 360f * animProgress.value

                    drawCircle(
                        color = newColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeW)
                    )

                    if (total > 0 && totalSweep > 0) {
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Total Words",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
}

// ─── Activity Chart (Last 30 Days Bar Chart) ─────────────────

@Composable
fun PremiumActivityChart(data: List<Int>) {
    val chronologicalData = data.reversed()
    val maxVal = chronologicalData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val numDays = data.size
    
    val barBrush = Brush.verticalGradient(
        colors = listOf(
            AnalyticsYellow,
            AnalyticsOrange
        )
    )
    val gridLineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000, delayMillis = 200))
    }

    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    val labels = List(numDays) { i ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(numDays - 1 - i))
        dateFormat.format(cal.time)
    }
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(chronologicalData) {
        scrollState.animateScrollTo(scrollState.maxValue, animationSpec = tween(500))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = AnalyticsYellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Activity (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            val itemWidth = 54.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(itemWidth * numDays)
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasHeight = size.height
                        
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = canvasHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }
                        
                        val barWidthPx = 14.dp.toPx()
                        val spacingPx = itemWidth.toPx()
                        
                        for (i in 0 until numDays) {
                            val value = chronologicalData[i]
                            val heightRatio = (value.toFloat() / maxVal) * animProgress.value
                            val barHeight = canvasHeight * heightRatio
                            
                            val x = (i * spacingPx) + (spacingPx - barWidthPx) / 2
                            val y = canvasHeight - barHeight
                            
                            if (value > 0) {
                                drawRoundRect(
                                    brush = barBrush,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidthPx, barHeight),
                                    cornerRadius = CornerRadius(barWidthPx / 2, barWidthPx / 2)
                                )
                            } else {
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
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState, enabled = false)
            ) {
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
                                color = AnalyticsYellow
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ─── Legend Item ──────────────────────────────────────────────

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
