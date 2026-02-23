package com.behtar.lens.internal.presentation.network

import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.data.model.WebViewLogEntry

/**
 * Sealed class representing all possible user events in the Network Inspector.
 *
 * Following MVI pattern, all user actions are represented as events that flow up to the ViewModel,
 * which then updates the state accordingly.
 */
sealed class NetworkEvent {

  // ========================================================================
  // Tab Navigation Events
  // ========================================================================

  /** User selected a tab in the main tab bar. */
  data class SelectTab(val tab: NetworkInspectorTab) : NetworkEvent()

  /** User selected a tab in the HTTP detail screen. */
  data class SelectHttpDetailTab(val tab: HttpDetailTab) : NetworkEvent()

  // ========================================================================
  // Entry Selection Events (Navigation to Detail)
  // ========================================================================

  /** User tapped on an HTTP log entry. */
  data class SelectHttpEntry(val entry: NetworkLogEntry) : NetworkEvent()

  /** User tapped on a WebView log entry. */
  data class SelectWebViewEntry(val entry: WebViewLogEntry) : NetworkEvent()

  /** User tapped on a WebSocket log entry. */
  data class SelectWebSocketEntry(val entry: WebSocketLogEntry) : NetworkEvent()

  // ========================================================================
  // Navigation Events
  // ========================================================================

  /** User pressed back button. */
  data object NavigateBack : NetworkEvent()

  // ========================================================================
  // Data Operation Events
  // ========================================================================

  /** User requested to clear logs for current tab. */
  data object ClearCurrentLogs : NetworkEvent()

  /** User requested to clear all logs. */
  data object ClearAllLogs : NetworkEvent()
}
