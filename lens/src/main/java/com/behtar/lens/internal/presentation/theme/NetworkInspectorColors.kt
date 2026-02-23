package com.behtar.lens.internal.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for JSON syntax highlighting.
 *
 * Uses Catppuccin Mocha theme by default - a popular color scheme designed for code readability in
 * dark mode.
 */
@Immutable
data class JsonSyntaxColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val nullValue: Color,
    val brace: Color,
    val text: Color,
    val background: Color,
    val headerBackground: Color,
    val lineNumber: Color,
    val divider: Color
) {
  companion object {
    val Default =
        JsonSyntaxColors(
            key = Color(0xFF89B4FA),
            string = Color(0xFFA6E3A1),
            number = Color(0xFFFAB387),
            boolean = Color(0xFFCBA6F7),
            nullValue = Color(0xFFF38BA8),
            brace = Color(0xFFF9E2AF),
            text = Color(0xFFCDD6F4),
            background = Color(0xFF1E1E2E),
            headerBackground = Color(0xFF313244),
            lineNumber = Color(0xFF6C7086),
            divider = Color(0xFF313244))
  }
}

/** Color scheme for HTTP method badges. */
object HttpMethodColors {
  val get = Color(0xFF4CAF50)
  val post = Color(0xFF2196F3)
  val put = Color(0xFFFF9800)
  val delete = Color(0xFFF44336)
  val patch = Color(0xFF9C27B0)
  val default = Color(0xFF607D8B)

  fun forMethod(method: String): Color =
      when (method.uppercase()) {
        "GET" -> get
        "POST" -> post
        "PUT" -> put
        "DELETE" -> delete
        "PATCH" -> patch
        else -> default
      }
}

/** Color scheme for HTTP status codes. */
object HttpStatusColors {
  val success = Color(0xFF4CAF50)
  val redirect = Color(0xFF2196F3)
  val clientError = Color(0xFFFF9800)
  val serverError = Color(0xFFF44336)
  val pending = Color(0xFF9E9E9E)
  val unknown = Color(0xFF9E9E9E)

  fun forStatusCode(code: Int?): Color =
      when {
        code == null -> pending
        code in 200..299 -> success
        code in 300..399 -> redirect
        code in 400..499 -> clientError
        code >= 500 -> serverError
        else -> unknown
      }
}

/** Color scheme for WebSocket connection status. */
object WebSocketStatusColors {
  val connecting = Color(0xFFFF9800)
  val open = Color(0xFF4CAF50)
  val closing = Color(0xFF2196F3)
  val closed = Color(0xFF9E9E9E)
  val failed = Color(0xFFF44336)
  val sent = Color(0xFF1E88E5)
  val received = Color(0xFF43A047)
}

/** General UI colors for the Network Inspector. */
object NetworkInspectorColors {
  val info = Color(0xFF2196F3)
  val warning = Color(0xFFFF9800)
  val error = Color(0xFFF44336)
  val success = Color(0xFF4CAF50)
}

val LocalJsonSyntaxColors = staticCompositionLocalOf { JsonSyntaxColors.Default }
