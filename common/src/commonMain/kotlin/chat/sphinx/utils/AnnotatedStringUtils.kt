package chat.sphinx.utils

import Roboto
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import chat.sphinx.utils.linkify.SphinxLinkify
import chat.sphinx.utils.linkify.urlSpanStyle

/**
 * Turn string to an annotated string (with clickable/highlighted text).
 */
// Define styles for different types of markdown
private val urlSpanStyle = SpanStyle(
    color = Color.Blue,
    textDecoration = TextDecoration.Underline
)

private val boldSpanStyle = SpanStyle(
    fontWeight = FontWeight.Bold
)

private val highlightSpanStyle = SpanStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Thin,
    background = Color(0x26FFFFFF)
)
/**
 * Turn string into an annotated string with clickable links, bold text, and highlighted text.
 */
fun String.toAnnotatedString(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var currentIndex = 0

    // Define regex patterns
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    val highlightRegex = "`([^`]*)`".toRegex()

    // Find all matches for bold and highlighted text, associating each with its type
    val matches = (boldRegex.findAll(this).map { it to "bold" } +
            highlightRegex.findAll(this).map { it to "highlight" })
        .sortedBy { it.first.range.first }

    matches.forEach { (matchResult, matchType) ->
        val start = matchResult.range.first
        val end = matchResult.range.last + 1
        val styledText = matchResult.groups[1]?.value ?: ""

        // Add preceding plain text
        if (currentIndex < start) {
            builder.append(this.substring(currentIndex, start))
        }

        // Add styled text
        builder.append(styledText)

        val style = when (matchType) {
            "bold" -> boldSpanStyle
            "highlight" -> highlightSpanStyle
            else -> null
        }

        style?.let {
            builder.addStyle(
                style = it,
                start = builder.length - styledText.length,
                end = builder.length
            )
        }

        // Update current index to the end of the matched range
        currentIndex = end
    }

    // Add any remaining plain text
    if (currentIndex < this.length) {
        builder.append(this.substring(currentIndex))
    }

    // Handle links using SphinxLinkify
    val links = SphinxLinkify.gatherLinks(
        text = this,
        mask = SphinxLinkify.ALL
    )
    links.forEach { linkSpec ->
        builder.addStyle(
            style = urlSpanStyle,
            start = linkSpec.start,
            end = linkSpec.end
        )
        builder.addStringAnnotation(
            tag = linkSpec.tag,
            annotation = linkSpec.url,
            start = linkSpec.start,
            end = linkSpec.end
        )
    }

    return builder.toAnnotatedString()
}

/**
 * Check if the string contains links with preview.
 */
fun String.containLinksWithPreview(): Boolean {
    val links = SphinxLinkify.gatherLinks(
        text = this,
        mask = SphinxLinkify.LINKS_WITH_PREVIEWS
    )
    return links.isNotEmpty()
}