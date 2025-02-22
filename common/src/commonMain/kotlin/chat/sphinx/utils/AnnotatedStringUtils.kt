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
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()  // Matches text wrapped in ** (bold)
    val highlightRegex = "(.*?)".toRegex()         // Matches any text (non-greedy)

    // Find all matches for bold and highlighted text, associating each with its type
    val matches = (boldRegex.findAll(this).map { it to "bold" } +
            highlightRegex.findAll(this).map { it to "highlight" })
        .sortedBy { it.first.range.first }

    matches.forEach { (matchResult, matchType) ->
        val start = matchResult.range.first.coerceIn(0, this.length)
        val end = (matchResult.range.last + 1).coerceIn(0, this.length)

        if (start >= end) return@forEach // Skip invalid ranges

        val styledText = matchResult.groups[1]?.value.orEmpty()

        // Add preceding plain text
        if (currentIndex < start) {
            builder.append(this.substring(currentIndex, start.coerceIn(0, this.length)))
        }

        // Add styled text
        builder.append(styledText)

        val style = when (matchType) {
            "bold" -> boldSpanStyle
            "highlight" -> highlightSpanStyle
            else -> null
        }

        style?.let {
            val styleStart = (builder.length - styledText.length).coerceAtLeast(0)
            val styleEnd = builder.length.coerceIn(0, builder.length)
            if (styleStart < styleEnd) {
                builder.addStyle(
                    style = it,
                    start = styleStart,
                    end = styleEnd
                )
            }
        }

        // Update current index to the end of the matched range
        currentIndex = end
    }

    // Add any remaining plain text
    if (currentIndex < this.length) {
        builder.append(this.substring(currentIndex.coerceIn(0, this.length)))
    }

    // Handle links using SphinxLinkify
    val links = SphinxLinkify.gatherLinks(
        text = this,
        mask = SphinxLinkify.ALL
    )
    links.forEach { linkSpec ->
        val linkStart = linkSpec.start.coerceIn(0, this.length)
        val linkEnd = linkSpec.end.coerceIn(0, this.length)

        if (linkStart < linkEnd) {
            builder.addStyle(
                style = urlSpanStyle,
                start = linkStart,
                end = linkEnd
            )
            builder.addStringAnnotation(
                tag = linkSpec.tag,
                annotation = linkSpec.url,
                start = linkStart,
                end = linkEnd
            )
        }
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

fun String.containsWebUrl(): Boolean {
    val links = SphinxLinkify.gatherLinks(
        text = this,
        mask = SphinxLinkify.WEB_URLS
    )
    return links.isNotEmpty()
}
