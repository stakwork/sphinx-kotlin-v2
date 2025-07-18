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

    val rawText = this
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    val highlightRegex = "`([^`]*)`".toRegex()

    val matches = (boldRegex.findAll(rawText).map { it to "bold" } +
            highlightRegex.findAll(rawText).map { it to "highlight" })
        .sortedBy { it.first.range.first }

    val positionMap = mutableListOf<Pair<IntRange, IntRange>>()  // maps raw -> builder

    matches.forEach { (matchResult, matchType) ->
        val fullRange = matchResult.range
        val styledText = matchResult.groups[1]?.value.orEmpty()

        // Append any plain text before this match
        if (currentIndex < fullRange.first) {
            val textToAppend = rawText.substring(currentIndex, fullRange.first)
            builder.append(textToAppend)
        }

        val start = builder.length
        builder.append(styledText)
        val end = builder.length

        val style = when (matchType) {
            "bold" -> boldSpanStyle
            "highlight" -> highlightSpanStyle
            else -> null
        }
        style?.let {
            builder.addStyle(it, start, end)
        }

        positionMap.add(fullRange to (start until end))
        currentIndex = fullRange.last + 1
    }

    if (currentIndex < rawText.length) {
        builder.append(rawText.substring(currentIndex))
    }

    // Reconstruct the clean text used in builder
    val builderText = builder.toString()

    // Run linkify on cleaned builder text
    val links = SphinxLinkify.gatherLinks(
        text = builderText,
        mask = SphinxLinkify.ALL
    )
    links.forEach { linkSpec ->
        val linkStart = linkSpec.start.coerceIn(0, builderText.length)
        val linkEnd = linkSpec.end.coerceIn(0, builderText.length)

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
