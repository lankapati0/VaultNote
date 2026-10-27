package com.lankeshwar.vaultnote.Utils
import androidx.compose.ui.text.AnnotatedString
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontStyle
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat


object RichTextUtils {

    fun toHtml(text: AnnotatedString): String {
        val spannable = SpannableString(text.text)

        text.spanStyles.forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)

            if (start >= end) return@forEach

            val style = span.item

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

            if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
                spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
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
}