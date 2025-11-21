package chat.sphinx.utils

import Roboto
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import chat.sphinx.utils.linkify.SphinxLinkify
import chat.sphinx.utils.linkify.urlSpanStyle
import theme.primary_green

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
private val searchHighlightSpanStyle = SpanStyle(
    background = primary_green,
    color = Color.Black
)
/**
 * Turn string into an annotated string with clickable links, bold text, and highlighted text.
 */
fun String.toAnnotatedString(searchQuery: String = ""): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var currentIndex = 0

    val rawText = this
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    val highlightRegex = "`([^`]*)`".toRegex()

    val matches = (boldRegex.findAll(rawText).map { it to "bold" } +
            highlightRegex.findAll(rawText).map { it to "highlight" })
        .sortedBy { it.first.range.first }

    // Keeps track of position mapping from rawText index -> builder index
    val rawToBuilderIndexMap = mutableMapOf<Int, Int>()

    var rawIndex = 0
    var builderIndex = 0

    fun appendAndMap(text: String) {
        for (i in text.indices) {
            rawToBuilderIndexMap[rawIndex++] = builderIndex++
        }
        builder.append(text)
    }

    matches.forEach { (matchResult, matchType) ->
        val fullRange = matchResult.range
        val styledText = matchResult.groups[1]?.value.orEmpty()

        if (currentIndex < fullRange.first) {
            val plainText = rawText.substring(currentIndex, fullRange.first)
            appendAndMap(plainText)
        }

        val start = builder.length
        appendAndMap(styledText)
        val end = builder.length

        val style = when (matchType) {
            "bold" -> boldSpanStyle
            "highlight" -> highlightSpanStyle
            else -> null
        }
        style?.let {
            builder.addStyle(it, start, end)
        }

        currentIndex = fullRange.last + 1
    }

    if (currentIndex < rawText.length) {
        val remaining = rawText.substring(currentIndex)
        appendAndMap(remaining)
    }

    // Linkify original raw text
    val links = SphinxLinkify.gatherLinks(
        text = rawText,
        mask = SphinxLinkify.ALL
    )

    links.forEach { link ->
        val rawStart = link.start
        val rawEnd = link.end

        val mappedStart = rawToBuilderIndexMap[rawStart] ?: return@forEach
        val mappedEnd = rawToBuilderIndexMap[rawEnd - 1]?.plus(1) ?: return@forEach

        if (mappedStart < mappedEnd) {
            builder.addStyle(
                style = urlSpanStyle,
                start = mappedStart,
                end = mappedEnd
            )
            builder.addStringAnnotation(
                tag = link.tag,
                annotation = link.url,
                start = mappedStart,
                end = mappedEnd
            )
        }
    }

    if (searchQuery.isNotBlank()) {
        val searchRegex = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
        searchRegex.findAll(builder.toAnnotatedString().text).forEach { matchResult ->
            builder.addStyle(
                style = searchHighlightSpanStyle,
                start = matchResult.range.first,
                end = matchResult.range.last + 1
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
