package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotatedString = rememberHighlightedText(text, query)
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun rememberHighlightedText(text: String, query: String): AnnotatedString {
    val highlightColor = MaterialTheme.colorScheme.primary
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        val lowerText = text.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())
        var startIndex = 0

        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }

            append(text.substring(startIndex, index))
            pushStyle(
                SpanStyle(
                    color = highlightColor,
                    background = highlightBg,
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(index, index + query.length))
            pop()
            startIndex = index + query.length
        }
    }
}
