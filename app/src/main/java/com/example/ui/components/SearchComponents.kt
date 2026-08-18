package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.Elevation
import com.example.ui.theme.Spacing

// 1. Elegant Custom SearchBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: ((String) -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = "Vocabulary Search Input Field" },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        leadingIcon = leadingIcon ?: {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = trailingIcon ?: {
            if (query.isNotEmpty() && onClearClick != null) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        onClearClick()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (onSearch != null) {
                    onSearch(query)
                }
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = CornerRadius.extraLarge
    )
}

// 2. Premium AnimatedSearchBar (Expands dynamically with beautiful animations)
@Composable
fun AnimatedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: ((String) -> Unit)? = null,
    onClearClick: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val widthPercent by animateDpAsState(
        targetValue = if (isExpanded || query.isNotEmpty()) 320.dp else 48.dp,
        animationSpec = tween(300), label = "searchWidth"
    )

    Row(
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = "Animated expandable search bar" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .width(widthPercent)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isExpanded || query.isNotEmpty()) {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = placeholder,
                    onSearch = onSearch,
                    onClearClick = {
                        onClearClick?.invoke()
                        isExpanded = false
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (query.isEmpty()) {
                                    isExpanded = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Active Search icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    onClick = { isExpanded = true },
                    modifier = Modifier.size(48.dp),
                    shape = CornerRadius.full,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = Elevation.level1
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Expand Search Bar Input",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
