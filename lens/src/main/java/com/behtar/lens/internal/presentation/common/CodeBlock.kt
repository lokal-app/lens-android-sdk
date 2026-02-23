package com.behtar.lens.internal.presentation.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.behtar.lens.internal.presentation.theme.JsonSyntaxColors
import com.behtar.lens.internal.presentation.theme.LocalJsonSyntaxColors

/**
 * Code block with JSON syntax highlighting. Supports copy to clipboard and horizontal/vertical
 * scrolling.
 */
@Composable
fun CodeBlock(
    content: String,
    modifier: Modifier = Modifier,
    colors: JsonSyntaxColors = LocalJsonSyntaxColors.current
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val formattedContent = remember(content) { formatJson(content) }
  val highlightedText =
      remember(formattedContent, colors) { highlightJson(formattedContent, colors) }

  Column(
      modifier = modifier.fillMaxWidth().background(colors.background, RoundedCornerShape(8.dp))) {
        // Header with copy button
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        colors.headerBackground, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "JSON",
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.text.copy(alpha = 0.7f))
              Spacer(modifier = Modifier.weight(1f))
              IconButton(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(content))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.text.copy(alpha = 0.7f))
                  }
            }

        // Content area
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)) {
              SelectionContainer {
                Text(
                    text = highlightedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp)
              }
            }
      }
}

/** Format JSON string with indentation. */
private fun formatJson(json: String): String {
  return try {
    val trimmed = json.trim()
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      prettyPrintJson(trimmed)
    } else {
      json
    }
  } catch (e: Exception) {
    json
  }
}

/** Simple JSON pretty printer. */
private fun prettyPrintJson(json: String): String {
  val sb = StringBuilder()
  var indent = 0
  var inString = false
  var escaped = false

  for (char in json) {
    when {
      escaped -> {
        sb.append(char)
        escaped = false
      }
      char == '\\' -> {
        sb.append(char)
        escaped = true
      }
      char == '"' -> {
        sb.append(char)
        inString = !inString
      }
      !inString ->
          when (char) {
            '{',
            '[' -> {
              sb.append(char)
              indent++
              sb.append('\n')
              sb.append("  ".repeat(indent))
            }
            '}',
            ']' -> {
              indent--
              sb.append('\n')
              sb.append("  ".repeat(indent))
              sb.append(char)
            }
            ',' -> {
              sb.append(char)
              sb.append('\n')
              sb.append("  ".repeat(indent))
            }
            ':' -> {
              sb.append(": ")
            }
            ' ',
            '\n',
            '\r',
            '\t' -> {
              /* skip whitespace */
            }
            else -> sb.append(char)
          }
      else -> sb.append(char)
    }
  }
  return sb.toString()
}

/** Apply syntax highlighting to JSON text. */
private fun highlightJson(json: String, colors: JsonSyntaxColors): AnnotatedString {
  return buildAnnotatedString {
    var i = 0
    var inString = false
    var isKey = true
    var escaped = false

    while (i < json.length) {
      val char = json[i]

      when {
        escaped -> {
          append(char)
          escaped = false
        }
        char == '\\' -> {
          append(char)
          escaped = true
        }
        char == '"' -> {
          val endQuote = findEndOfString(json, i + 1)
          val stringContent = json.substring(i, endQuote + 1)

          val color = if (isKey) colors.key else colors.string
          withStyle(SpanStyle(color = color)) { append(stringContent) }

          i = endQuote
          inString = false
          isKey = !isKey
        }
        char == ':' -> {
          withStyle(SpanStyle(color = colors.text)) { append(char) }
          isKey = false
        }
        char == ',' -> {
          withStyle(SpanStyle(color = colors.text)) { append(char) }
          isKey = true
        }
        char in "{[]}" -> {
          withStyle(SpanStyle(color = colors.brace)) { append(char) }
          if (char == '{') isKey = true
        }
        char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
          val numEnd = findEndOfNumber(json, i)
          withStyle(SpanStyle(color = colors.number)) { append(json.substring(i, numEnd)) }
          i = numEnd - 1
        }
        json.startsWith("true", i) -> {
          withStyle(SpanStyle(color = colors.boolean)) { append("true") }
          i += 3
        }
        json.startsWith("false", i) -> {
          withStyle(SpanStyle(color = colors.boolean)) { append("false") }
          i += 4
        }
        json.startsWith("null", i) -> {
          withStyle(SpanStyle(color = colors.nullValue)) { append("null") }
          i += 3
        }
        else -> withStyle(SpanStyle(color = colors.text)) { append(char) }
      }
      i++
    }
  }
}

private fun findEndOfString(json: String, start: Int): Int {
  var i = start
  var escaped = false
  while (i < json.length) {
    val char = json[i]
    if (escaped) {
      escaped = false
    } else if (char == '\\') {
      escaped = true
    } else if (char == '"') {
      return i
    }
    i++
  }
  return json.length - 1
}

private fun findEndOfNumber(json: String, start: Int): Int {
  var i = start
  while (i < json.length && (json[i].isDigit() || json[i] in ".-+eE")) {
    i++
  }
  return i
}
