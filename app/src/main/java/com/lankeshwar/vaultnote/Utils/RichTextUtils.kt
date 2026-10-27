package com.lankeshwar.vaultnote.Utils
import androidx.compose.ui.text.AnnotatedString
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontStyle
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.isSpecified
import androidx.core.text.HtmlCompat
import androidx.compose.ui.unit.sp

object RichTextUtils {

    fun applyStyle(
        text: AnnotatedString,
        start: Int,
        end: Int,
        requestedStyle: SpanStyle
    ): AnnotatedString {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(0, text.length)

        if (safeStart >= safeEnd) return text

        fun mergeStyles(
            existing: SpanStyle,
            requested: SpanStyle
        ): SpanStyle {
            val merged = existing.merge(requested)

            val decorations = listOfNotNull(
                existing.textDecoration,
                requested.textDecoration
            )

            return merged.copy(
                textDecoration = if (decorations.isEmpty()) {
                    null
                } else {
                    TextDecoration.combine(decorations)
                }
            )
        }

        val boundaries = (
                listOf(0, text.length, safeStart, safeEnd) +
                        text.spanStyles.flatMap { listOf(it.start, it.end) }
                )
            .map { it.coerceIn(0, text.length) }
            .distinct()
            .sorted()

        val builder = AnnotatedString.Builder()

        for (i in 0 until boundaries.lastIndex) {
            val segmentStart = boundaries[i]
            val segmentEnd = boundaries[i + 1]

            if (segmentStart >= segmentEnd) continue

            var combined = SpanStyle()

            text.spanStyles
                .filter {
                    it.start <= segmentStart &&
                            it.end >= segmentEnd
                }
                .forEach {
                    combined = mergeStyles(combined, it.item)
                }

            if (segmentStart >= safeStart && segmentEnd <= safeEnd) {
                combined = mergeStyles(combined, requestedStyle)
            }

            builder.pushStyle(combined)
            builder.append(text.text.substring(segmentStart, segmentEnd))
            builder.pop()
        }

        return builder.toAnnotatedString()
    }


    fun toHtml(text: AnnotatedString): String {
        val spannable = SpannableString(text.text)

        val boundaries = (
                text.spanStyles.flatMap { listOf(it.start, it.end) } +
                        listOf(0, text.length)
                )
            .map { it.coerceIn(0, text.length) }
            .distinct()
            .sorted()

        for (i in 0 until boundaries.lastIndex) {
            val start = boundaries[i]
            val end = boundaries[i + 1]

            if (start >= end) continue

            // Combine all styles that cover this text segment.
            var combinedStyle = SpanStyle()

            text.spanStyles
                .filter { it.start <= start && it.end >= end }
                .forEach { span ->
                    combinedStyle = combinedStyle.merge(span.item)
                }

            val style = combinedStyle

            if (style.fontSize.isSpecified) {
                spannable.setSpan(
                    AbsoluteSizeSpan(style.fontSize.value.toInt(), true),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val isBold = style.fontWeight == FontWeight.Bold
            val isItalic = style.fontStyle == FontStyle.Italic

            if (isBold || isItalic) {
                val typefaceStyle = when {
                    isBold && isItalic -> Typeface.BOLD_ITALIC
                    isBold -> Typeface.BOLD
                    else -> Typeface.ITALIC
                }

                spannable.setSpan(
                    StyleSpan(typefaceStyle),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (style.textDecoration?.contains(
                    TextDecoration.Underline
                ) == true
            ) {
                spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (style.textDecoration?.contains(
                    TextDecoration.LineThrough
                ) == true
            ) {
                spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return HtmlCompat.toHtml(
            spannable,
            HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
        )
    }


    fun fromHtml(html: String): TextFieldValue {
        val spanned = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        val builder = AnnotatedString.Builder()
        builder.append(spanned.toString())

        spanned.getSpans(
            0, spanned.length, Any::class.java
        ).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            if (start < 0 || end <= start) return@forEach

            val style = when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> SpanStyle(
                        fontWeight = FontWeight.Bold
                    )
                    Typeface.ITALIC -> SpanStyle(
                        fontStyle = FontStyle.Italic
                    )
                    Typeface.BOLD_ITALIC -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                    else -> null
                }

                is UnderlineSpan -> SpanStyle(
                    textDecoration = TextDecoration.Underline
                )

                is StrikethroughSpan -> SpanStyle(
                    textDecoration = TextDecoration.LineThrough
                )
                is AbsoluteSizeSpan -> SpanStyle(
                    fontSize = span.size.sp
                )
                is RelativeSizeSpan -> SpanStyle(
                    fontSize = (16 * span.sizeChange).sp
                )
                else -> null
            }

            if (style != null) {
                builder.addStyle(
                    style = style,
                    start = start.coerceIn(0, builder.length),
                    end = end.coerceIn(0, builder.length)
                )
            }
        }

        return TextFieldValue(
            annotatedString = builder.toAnnotatedString(),
            selection = TextRange(0)
        )
    }



    fun markdownToHtml(markdown: String): String {
        val result = StringBuilder()
        var inList = false

        fun closeList() {
            if (inList) {
                result.append("</ul>")
                inList = false
            }
        }

        markdown.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("### ") -> {
                    closeList()
                    result.append("<h3>${escapeMarkdownHtml(trimmed.substring(4))}</h3>")
                }

                trimmed.startsWith("## ") -> {
                    closeList()
                    result.append("<h2>${escapeMarkdownHtml(trimmed.substring(3))}</h2>")
                }

                trimmed.startsWith("# ") -> {
                    closeList()
                    result.append("<h1>${escapeMarkdownHtml(trimmed.substring(2))}</h1>")
                }

                trimmed.startsWith("- [ ] ") -> {
                    closeList()
                    result.append("&#9744; ${escapeMarkdownHtml(trimmed.substring(6))}<br>")
                }

                trimmed.startsWith("- [x] ", ignoreCase = true) -> {
                    closeList()
                    result.append("&#9745; ${escapeMarkdownHtml(trimmed.substring(6))}<br>")
                }

                trimmed.startsWith("- ") -> {
                    if (!inList) {
                        result.append("<ul>")
                        inList = true
                    }

                    result.append(
                        "<li>${escapeMarkdownHtml(trimmed.substring(2))}</li>"
                    )
                }

                Regex("""^(\d+)\.\s+(.+)$""").matchEntire(trimmed) != null -> {
                    closeList()

                    val match = Regex("""^(\d+)\.\s+(.+)$""")
                        .matchEntire(trimmed)!!

                    result.append(
                        "${match.groupValues[1]}. " +
                                "${escapeMarkdownHtml(match.groupValues[2])}<br>"
                    )
                }

                trimmed.isEmpty() -> {
                    closeList()
                    result.append("<br>")
                }

                else -> {
                    closeList()
                    result.append("${escapeMarkdownHtml(trimmed)}<br>")
                }
            }
        }

        closeList()

        return result.toString()
            .replace(Regex("""\*\*(.+?)\*\*"""), "<b>$1</b>")
            .replace(Regex("""\*(.+?)\*"""), "<i>$1</i>")
    }

    private fun escapeMarkdownHtml(text: String): String {
        return android.text.Html.escapeHtml(text)
    }




}