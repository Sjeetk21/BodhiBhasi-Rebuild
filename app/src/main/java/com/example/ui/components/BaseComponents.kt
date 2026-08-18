package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.Elevation
import com.example.ui.theme.Spacing
import com.example.ui.theme.customColors

// 1. Primary Button
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "$text button" },
        enabled = enabled,
        shape = CornerRadius.large,
        colors = colors,
        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

// 2. Secondary Button
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "$text secondary button" },
        enabled = enabled,
        shape = CornerRadius.large,
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

// 3. Section Header
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// 4. Chapter Chip
@Composable
fun ChapterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150), label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150), label = "chipText"
    )

    Surface(
        modifier = modifier
            .clip(CornerRadius.medium)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Chapter $name chip. selected: $isSelected" },
        color = containerColor,
        contentColor = contentColor,
        shape = CornerRadius.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 5. Topic Chip
@Composable
fun TopicChip(
    topic: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier.clip(CornerRadius.small),
        color = color,
        contentColor = textColor,
        shape = CornerRadius.small
    ) {
        Text(
            text = topic.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp)
        )
    }
}

// 6. Custom Bottom Navigation Bar
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = containerColor,
        tonalElevation = Elevation.level2,
        content = content
    )
}

// 7. Top App Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        colors = colors,
        modifier = modifier
    )
}

// 8. Large Top App Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

// 9. Floating Action Button
@Composable
fun PremiumFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CornerRadius.large,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(Elevation.level3),
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

// 10. Custom Divider
@Composable
fun CustomDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    thickness: Dp = 1.dp
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

// 11. Confirmation Dialog
@Composable
fun ConfirmationDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = confirmText,
                    onClick = onConfirm
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = dismissText,
                    onClick = onDismiss
                )
            },
            shape = CornerRadius.extraLarge,
            modifier = modifier
        )
    }
}

// 12. Elegant Custom Snackbar
@Composable
fun ElegantSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnRight: Boolean = true
) {
    Card(
        shape = CornerRadius.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level3),
        modifier = modifier
            .padding(Spacing.medium)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = snackbarData.visuals.message,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            snackbarData.visuals.actionLabel?.let { label ->
                Spacer(modifier = Modifier.width(Spacing.small))
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.inversePrimary
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// 13. Action Button: Pronunciation Button
@Composable
fun PronunciationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = false
) {
    val scale by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 24.dp,
        animationSpec = tween(150), label = "scalePlay"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = "Play pronunciation audio" }
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(scale)
        )
    }
}

// 14. Action Button: Bookmark Button
@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isBookmarked) {
        MaterialTheme.customColors.bookmark
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark this word" }
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

// 15. Action Button: Copy Button
@Composable
fun CopyButton(
    textToCopy: String,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(1500)
            isCopied = false
        }
    }

    IconButton(
        onClick = {
            isCopied = true
            onCopied()
        },
        modifier = modifier
            .semantics { contentDescription = "Copy text to clipboard" }
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
            contentDescription = null,
            tint = if (isCopied) MaterialTheme.customColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

// 16. Action Button: Share Button
@Composable
fun ShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = "Share word definition" }
            .minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
