package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.Elevation
import com.example.ui.theme.Spacing

// 1. Shimmer Brush Modifier
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    background(brush = brush)
}

// 2. Base Shimmer Rectangle
@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CornerRadius.small
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmer()
    )
}

// 3. LoadingView (Centered Spinner)
@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// 4. Skeleton Word Card (Placeholder)
@Composable
fun SkeletonWordCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.medium)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic Chip placeholder
                ShimmerItem(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp),
                    shape = CornerRadius.small
                )
                // Favorite placeholder
                ShimmerItem(
                    modifier = Modifier
                        .size(24.dp),
                    shape = CornerRadius.full
                )
            }
            Spacer(modifier = Modifier.height(Spacing.small))
            // Word title placeholder
            ShimmerItem(
                modifier = Modifier
                    .width(160.dp)
                    .height(28.dp),
                shape = CornerRadius.small
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            // Pronunciation placeholder
            ShimmerItem(
                modifier = Modifier
                    .width(100.dp)
                    .height(16.dp),
                shape = CornerRadius.small
            )
            Spacer(modifier = Modifier.height(Spacing.medium))
            // Meaning line 1 placeholder
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp),
                shape = CornerRadius.small
            )
            Spacer(modifier = Modifier.height(Spacing.extraSmall))
            // Meaning line 2 placeholder
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp),
                shape = CornerRadius.small
            )
        }
    }
}

// 5. Word Detail Skeleton Placeholder
@Composable
fun SkeletonWordDetailPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.medium)
    ) {
        // Gradient header skeleton
        ShimmerItem(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = CornerRadius.large
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        // Meaning Title
        ShimmerItem(modifier = Modifier.width(120.dp).height(20.dp))
        Spacer(modifier = Modifier.height(Spacing.small))
        // Meaning Box
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(100.dp), shape = CornerRadius.large)
        Spacer(modifier = Modifier.height(Spacing.medium))
        // Examples Title
        ShimmerItem(modifier = Modifier.width(140.dp).height(20.dp))
        Spacer(modifier = Modifier.height(Spacing.small))
        // Example Box
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(80.dp), shape = CornerRadius.large)
        Spacer(modifier = Modifier.height(Spacing.medium))
        // Memory Hook Box
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(70.dp), shape = CornerRadius.large)
    }
}

// 6. Search Placeholder list
@Composable
fun SkeletonSearchPlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.medium)
    ) {
        items(4) {
            SkeletonWordCard()
        }
    }
}

// 7. Statistics Placeholder
@Composable
fun SkeletonStatisticsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.medium)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            ShimmerItem(modifier = Modifier.weight(1f).height(100.dp), shape = CornerRadius.large)
            ShimmerItem(modifier = Modifier.weight(1f).height(100.dp), shape = CornerRadius.large)
        }
        Spacer(modifier = Modifier.height(Spacing.medium))
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(140.dp), shape = CornerRadius.large)
        Spacer(modifier = Modifier.height(Spacing.medium))
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(200.dp), shape = CornerRadius.large)
    }
}

// 8. Elegant Custom EmptyState with Illustration Icon
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.FolderOpen,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CornerRadius.full,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(Spacing.large))
            actionButton()
        }
    }
}

// 9. Premium Error Card with helpful actions and dynamic messages
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    icon: ImageVector = Icons.Default.Info
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CornerRadius.full,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.error
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(Spacing.large))
        PrimaryButton(
            text = "Retry",
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        )
    }
}

// Custom specialized empty states
@Composable
fun NoSearchResultsState(query: String, modifier: Modifier = Modifier) {
    EmptyState(
        title = "No search results",
        message = "We couldn't find any results for \"$query\". Try checking the spelling or typing another term.",
        icon = Icons.Default.Search,
        modifier = modifier
    )
}

@Composable
fun NoFavoritesState(modifier: Modifier = Modifier, onExploreClick: (() -> Unit)? = null) {
    EmptyState(
        title = "Your saved words list is empty",
        message = "Words you save will appear here for quick revision.",
        icon = Icons.Default.StarBorder,
        modifier = modifier,
        actionButton = onExploreClick?.let {
            {
                PrimaryButton(
                    text = "Explore Dictionary",
                    onClick = it
                )
            }
        }
    )
}

@Composable
fun NoHistoryState(modifier: Modifier = Modifier) {
    EmptyState(
        title = "No reading history",
        message = "Words you view during your studies will be kept here for easy recall.",
        icon = Icons.Default.History,
        modifier = modifier
    )
}

@Composable
fun NoChaptersState(modifier: Modifier = Modifier, onSyncClick: (() -> Unit)? = null) {
    EmptyState(
        title = "No chapters found",
        message = "Please sync with the Google Sheet to populate your vocabulary list.",
        icon = Icons.Default.FolderOpen,
        modifier = modifier,
        actionButton = onSyncClick?.let {
            {
                PrimaryButton(
                    text = "Sync Now",
                    onClick = it
                )
            }
        }
    )
}

@Composable
fun NoInternetState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ErrorView(
        title = "Network offline",
        message = "An active internet connection is required to fetch sheet records. Please check your data or Wi-Fi connection.",
        icon = Icons.Default.CloudOff,
        onRetry = onRetry,
        modifier = modifier
    )
}

@Composable
fun SyncFailedState(errorMsg: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ErrorView(
        title = "Sync unsuccessful",
        message = "Failed to sync vocabulary records. Cause: $errorMsg",
        icon = Icons.Default.SyncProblem,
        onRetry = onRetry,
        modifier = modifier
    )
}
