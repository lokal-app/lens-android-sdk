package com.behtar.lens.internal.presentation.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.behtar.lens.internal.presentation.network.NetworkViewModel
import com.behtar.lens.internal.presentation.theme.JsonSyntaxColors
import com.behtar.lens.internal.presentation.theme.LocalJsonSyntaxColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Code block with JSON syntax highlighting. Supports copy to clipboard and horizontal/vertical
 * scrolling.
 *
 * Rendering is split into two async stages on [Dispatchers.Default] to keep the main thread free:
 * 1. Pretty-print (plain String → indented String) — result supplied by caller via
 *    [cachedFormatted] if already computed for this entry (ViewModel cache), otherwise computed
 *    here and handed back via [onFormatted] so the caller can cache it.
 * 2. Syntax-highlight (indented String → AnnotatedString) — always runs off-main-thread via
 *    [produceState]. Shows a spinner until complete.
 *
 * Copy always uses the full original [content], not the display-truncated version.
 */
@Composable
fun CodeBlock(
    content: String,
    modifier: Modifier = Modifier,
    cachedFormatted: String? = null,
    onFormatted: ((String) -> Unit)? = null,
    cachedHighlighted: AnnotatedString? = null,
    onHighlighted: ((AnnotatedString) -> Unit)? = null,
    colors: JsonSyntaxColors = LocalJsonSyntaxColors.current,
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  // Truncate only what we render — copy always uses the full original content.
  val displayContent =
      if (content.length > NetworkViewModel.DISPLAY_LIMIT)
          content.take(NetworkViewModel.DISPLAY_LIMIT)
      else content
  val isTruncated = content.length > NetworkViewModel.DISPLAY_LIMIT

  // If the caller supplies a pre-computed AnnotatedString, use it immediately (no spinner).
  // Otherwise: stage 1 (prettyPrint, skipped if cachedFormatted is set) and stage 2 (highlight)
  // both run off-main-thread via produceState, showing a spinner until complete.
  // The result is handed back via onHighlighted so the caller can cache it for future renders.
  val highlightedText by
      produceState<AnnotatedString?>(
          initialValue = cachedHighlighted,
          key1 = displayContent,
          key2 = cachedFormatted,
          key3 = cachedHighlighted,
      ) {
        if (cachedHighlighted != null) {
          value = cachedHighlighted
          return@produceState
        }
        value =
            withContext(Dispatchers.Default) {
              val formatted =
                  cachedFormatted
                      ?: run {
                        val result = formatJson(displayContent)
                        onFormatted?.invoke(result)
                        result
                      }
              val highlighted = highlightJson(formatted, colors)
              onHighlighted?.invoke(highlighted)
              highlighted
            }
      }

  Column(
      modifier = modifier.fillMaxWidth().background(colors.background, RoundedCornerShape(8.dp))) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        colors.headerBackground, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = if (isTruncated) "JSON (truncated — copy for full)" else "JSON",
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.text.copy(alpha = 0.7f))
              Spacer(modifier = Modifier.weight(1f))
              IconButton(
                  onClick = {
                    // Always copy the full original content, never the truncated display string.
                    clipboardManager.setText(AnnotatedString(content))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.text.copy(alpha = 0.7f))
                  }
            }

        if (highlightedText == null) {
          Box(
              modifier = Modifier.fillMaxWidth().height(120.dp),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.text.copy(alpha = 0.5f),
                    strokeWidth = 2.dp)
              }
        } else {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .weight(1f, fill = false)
                      .horizontalScroll(rememberScrollState())
                      .verticalScroll(rememberScrollState())
                      .padding(12.dp)) {
                SelectionContainer {
                  Text(
                      text = highlightedText!!,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 12.sp,
                      lineHeight = 18.sp)
                }
              }
        }
      }
}

internal fun formatJson(json: String): String {
  return try {
    val trimmed = json.trim()
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) prettyPrintJson(trimmed) else json
  } catch (e: Exception) {
    json
  }
}

// Pre-built indent strings up to depth 32. Eliminates "  ".repeat(n) string allocation
// on every structural character — the original hotspot in prettyPrintJson.
private val INDENT_CACHE: Array<String> = Array(33) { depth -> "  ".repeat(depth) }

private fun indent(depth: Int): String =
    if (depth < INDENT_CACHE.size) INDENT_CACHE[depth] else "  ".repeat(depth)

private fun prettyPrintJson(json: String): String {
  // Pre-size the builder generously to avoid mid-flight resizes on large inputs.
  val sb = StringBuilder(json.length + json.length / 4)
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
              sb.append(indent(indent))
            }
            '}',
            ']' -> {
              indent--
              sb.append('\n')
              sb.append(indent(indent))
              sb.append(char)
            }
            ',' -> {
              sb.append(char)
              sb.append('\n')
              sb.append(indent(indent))
            }
            ':' -> sb.append(": ")
            ' ',
            '\n',
            '\r',
            '\t' -> {} // skip whitespace outside strings
            else -> sb.append(char)
          }
      else -> sb.append(char)
    }
  }
  return sb.toString()
}

// Batched span highlighter: accumulates runs of same-color text and emits one span per run,
// instead of one span per character. Reduces span count by ~5–8x on typical JSON responses,
// which directly cuts AnnotatedString allocation size and first-render time.
private fun highlightJson(json: String, colors: JsonSyntaxColors): AnnotatedString {
  return buildAnnotatedString {
    var i = 0
    var isKey = true

    // Pending run of same-color characters — flushed when color changes or on structural tokens.
    val runBuffer = StringBuilder()
    var runColor = colors.text

    fun flushRun() {
      if (runBuffer.isEmpty()) return
      withStyle(SpanStyle(color = runColor)) { append(runBuffer) }
      runBuffer.clear()
    }

    fun appendColored(text: String, color: androidx.compose.ui.graphics.Color) {
      if (color != runColor) {
        flushRun()
        runColor = color
      }
      runBuffer.append(text)
    }

    fun appendColored(char: Char, color: androidx.compose.ui.graphics.Color) {
      if (color != runColor) {
        flushRun()
        runColor = color
      }
      runBuffer.append(char)
    }

    while (i < json.length) {
      val char = json[i]
      when {
        char == '"' -> {
          flushRun()
          val endQuote = findEndOfString(json, i + 1)
          val stringContent = json.substring(i, endQuote + 1)
          withStyle(SpanStyle(color = if (isKey) colors.key else colors.string)) {
            append(stringContent)
          }
          i = endQuote
          isKey = !isKey
        }
        char == ':' -> {
          flushRun()
          appendColored(char, colors.text)
          isKey = false
        }
        char == ',' -> {
          flushRun()
          appendColored(char, colors.text)
          isKey = true
        }
        char in "{[]}" -> {
          flushRun()
          appendColored(char, colors.brace)
          if (char == '{') isKey = true
        }
        char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
          flushRun()
          val numEnd = findEndOfNumber(json, i)
          appendColored(json.substring(i, numEnd), colors.number)
          i = numEnd - 1
        }
        json.startsWith("true", i) -> {
          flushRun()
          appendColored("true", colors.boolean)
          i += 3
        }
        json.startsWith("false", i) -> {
          flushRun()
          appendColored("false", colors.boolean)
          i += 4
        }
        json.startsWith("null", i) -> {
          flushRun()
          appendColored("null", colors.nullValue)
          i += 3
        }
        else -> appendColored(char, colors.text)
      }
      i++
    }
    flushRun()
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
