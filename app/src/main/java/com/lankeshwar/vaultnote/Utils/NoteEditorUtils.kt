package com.lankeshwar.vaultnote.Utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.util.fastCoerceAtLeast

object NoteEditorUtils {
    fun insertBullet(content: TextFieldValue): TextFieldValue {
        return insertListMarker(content, "• ")
    }

    fun insertChecklistItem(content: TextFieldValue): TextFieldValue {
        return insertListMarker(content, "☐ ")
    }

    fun insertListMarker(
        content: TextFieldValue,
        marker: String
    ): TextFieldValue {
        val text = content.text

        val start = content.selection.start.coerceIn(0, text.length)
        val end = content.selection.end.coerceIn(0, text.length)

        val selectionStart = minOf(start, end)
        val selectionEnd = maxOf(start, end)

        // Find the beginning of the selected line.
        val lineStart = if (selectionStart == 0) {
            0
        } else {
            text.lastIndexOf('\n', selectionStart - 1) + 1
        }

        // Avoid adding the same marker twice.
        if (text.startsWith(marker, lineStart)) {
            return content
        }

        val updatedText = buildString {
            append(text.substring(0, lineStart))
            append(marker)
            append(text.substring(lineStart))
        }

        return TextFieldValue(
            text = updatedText,
            selection = TextRange(
                selectionStart + marker.length,
                selectionEnd + marker.length
            )
        )
    }

    fun insertNumberedList(content: TextFieldValue): TextFieldValue {
        val text = content.text
        val start = content.selection.start.coerceIn(0, text.length)
        val end = content.selection.end.coerceIn(0, text.length)
        val lineStart = if (start == 0) 0
        else text.lastIndexOf('\n', start - 1) + 1

        // Don't add a second number to an already numbered line.
        val currentLine = text.substring(lineStart)
            .substringBefore('\n')

        if (Regex("""^\d+\.\s""").containsMatchIn(currentLine)) {
            return content
        }

        // Continue numbering from the previous line, if possible.
        val previousLine = if (lineStart > 0) {
            val previousEnd = lineStart - 1
            val previousStart = text.lastIndexOf('\n', previousEnd - 1) + 1
            text.substring(previousStart, previousEnd)
        } else {
            ""
        }

        val previousNumber = Regex("""^(\d+)\.\s+.+$""")
            .matchEntire(previousLine)
            ?.groupValues?.get(1)?.toIntOrNull()

        val marker = "${(previousNumber ?: 0) + 1}. "

        val updatedText = text.substring(0, lineStart) +
                marker + text.substring(lineStart)

        return TextFieldValue(
            text = updatedText,
            selection = TextRange(start + marker.length, end + marker.length)
        )
    }

    fun handleNumberedListInput(
        previous: TextFieldValue,
        current: TextFieldValue
    ): TextFieldValue {
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)

        // Only handle a single newly inserted newline.
        if (text.length != previous.text.length + 1 ||
            cursor == 0 ||
            text[cursor - 1] != '\n'
        ) {
            return current
        }

        val previousLineEnd = cursor - 1
        val previousLineStart =
            text.lastIndexOf('\n', previousLineEnd - 1) + 1
        val previousLine = text.substring(previousLineStart, previousLineEnd)

        // Continue only if the previous item has actual text.
        val match = Regex("""^(\d+)\.\s+.+$""")
            .matchEntire(previousLine) ?: return current

        val nextNumber = match.groupValues[1].toIntOrNull()
            ?.plus(1) ?: return current

        val marker = "$nextNumber. "
        val updatedText = text.substring(0, cursor) +
                marker + text.substring(cursor)

        return TextFieldValue(
            text = updatedText,
            selection = TextRange(cursor + marker.length)
        )
    }









}