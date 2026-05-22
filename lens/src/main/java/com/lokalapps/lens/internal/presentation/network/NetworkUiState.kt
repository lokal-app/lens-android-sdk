package com.lokalapps.lens.internal.presentation.network

import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import com.lokalapps.lens.internal.data.model.WebViewLogEntry

/**
 * Complete UI state for the Network Inspector screen.
 *
 * This is an immutable data class following Compose best practices:
 * - All properties are immutable (val)
 * - State changes create new instances via copy()
 * - Enables efficient recomposition through structural equality
 */
data class NetworkUiState(
    val selectedTab: NetworkInspectorTab = NetworkInspectorTab.API,
    val selectedScreen: SelectedScreen = SelectedScreen.List,
    val selectedHttpDetailTab: HttpDetailTab = HttpDetailTab.OVERVIEW
)

/** Available tabs in the Network Inspector. */
enum class NetworkInspectorTab(val title: String) {
  API("API"),
  WEBVIEW("WebView"),
  WEBSOCKET("WebSocket")
}

/** Available tabs in HTTP detail screen. */
enum class HttpDetailTab(val title: String) {
  OVERVIEW("Overview"),
  HEADERS("Headers"),
  REQUEST("Request"),
  RESPONSE("Response"),
  CURL("cURL")
}

/**
 * Represents the currently selected screen/navigation state.
 *
 * Using sealed class enables exhaustive when expressions and type-safe navigation handling.
 */
sealed class SelectedScreen {
  /** Main list screen */
  data object List : SelectedScreen()

  /** HTTP request detail screen */
  data class HttpDetail(val entry: NetworkLogEntry) : SelectedScreen()

  /** WebView request detail screen */
  data class WebViewDetail(val entry: WebViewLogEntry) : SelectedScreen()

  /** WebSocket connection detail screen */
  data class WebSocketDetail(val entry: WebSocketLogEntry) : SelectedScreen()
}
