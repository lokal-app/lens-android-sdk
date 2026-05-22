package com.lokalapps.lens.internal.presentation.network

import android.util.LruCache
import androidx.lifecycle.ViewModel
import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import com.lokalapps.lens.internal.data.model.WebViewLogEntry
import com.lokalapps.lens.internal.domain.usecase.network.ClearNetworkLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetNetworkLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetWebSocketLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetWebViewLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Network Inspector screen.
 *
 * Follows the MVI (Model-View-Intent) pattern:
 * - **Model**: [NetworkUiState] - immutable state
 * - **View**: Compose screens that observe state
 * - **Intent**: [NetworkEvent] - user actions
 *
 * All user interactions flow through [onEvent], which processes events and updates the state
 * accordingly.
 *
 * Dependencies are provided via [LensViewModelFactory].
 */
class NetworkViewModel(
    private val getNetworkLogsUseCase: GetNetworkLogsUseCase,
    private val getWebSocketLogsUseCase: GetWebSocketLogsUseCase,
    private val getWebViewLogsUseCase: GetWebViewLogsUseCase,
    private val clearNetworkLogsUseCase: ClearNetworkLogsUseCase
) : ViewModel() {

  private val _uiState = MutableStateFlow(NetworkUiState())
  val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

  // Expose repository flows directly for data
  // This maintains single source of truth without duplicating data
  val httpLogs: StateFlow<List<NetworkLogEntry>> = getNetworkLogsUseCase()
  val webViewLogs: StateFlow<List<WebViewLogEntry>> = getWebViewLogsUseCase()
  val webSocketLogs: StateFlow<List<WebSocketLogEntry>> = getWebSocketLogsUseCase()

  // Cache of pretty-printed (but not syntax-highlighted) response bodies, keyed by entry ID.
  // Stores plain Strings — no Compose/UI types — so the ViewModel stays UI-layer-free.
  // LruCache evicts the least-recently-used entry once the cap is reached. 100 entries at
  // DISPLAY_LIMIT (50KB) each = ~5MB worst-case, acceptable for a debug tool.
  // Cleared entirely on clearLogs so stale entries can't survive a log wipe.
  val formattedBodyCache = LruCache<String, String>(FORMATTED_CACHE_MAX_ENTRIES)

  companion object {
    // Maximum number of pre-formatted bodies to keep in memory at once.
    private const val FORMATTED_CACHE_MAX_ENTRIES = 100

    // Maximum characters rendered in CodeBlock. Copy always uses the full original string.
    const val DISPLAY_LIMIT = 50_000
  }

  /**
   * Single entry point for all user events.
   *
   * This enforces unidirectional data flow:
   * - User actions come in as events
   * - ViewModel processes events and updates state
   * - UI recomposes based on new state
   *
   * @param event The user event to process
   */
  fun onEvent(event: NetworkEvent) {
    when (event) {
      // Tab Navigation
      is NetworkEvent.SelectTab -> selectTab(event.tab)
      is NetworkEvent.SelectHttpDetailTab -> selectHttpDetailTab(event.tab)

      // Entry Selection
      is NetworkEvent.SelectHttpEntry -> selectHttpEntry(event.entry)
      is NetworkEvent.SelectWebViewEntry -> selectWebViewEntry(event.entry)
      is NetworkEvent.SelectWebSocketEntry -> selectWebSocketEntry(event.entry)

      // Navigation
      NetworkEvent.NavigateBack -> navigateBack()

      // Data Operations
      NetworkEvent.ClearCurrentLogs -> clearCurrentLogs()
      NetworkEvent.ClearAllLogs -> clearAllLogs()
    }
  }

  // ========================================================================
  // Tab Navigation
  // ========================================================================

  private fun selectTab(tab: NetworkInspectorTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  private fun selectHttpDetailTab(tab: HttpDetailTab) {
    _uiState.update { it.copy(selectedHttpDetailTab = tab) }
  }

  // ========================================================================
  // Entry Selection (Navigation to Detail)
  // ========================================================================

  private fun selectHttpEntry(entry: NetworkLogEntry) {
    _uiState.update {
      it.copy(
          selectedScreen = SelectedScreen.HttpDetail(entry),
          selectedHttpDetailTab = HttpDetailTab.OVERVIEW)
    }
  }

  private fun selectWebViewEntry(entry: WebViewLogEntry) {
    _uiState.update { it.copy(selectedScreen = SelectedScreen.WebViewDetail(entry)) }
  }

  private fun selectWebSocketEntry(entry: WebSocketLogEntry) {
    _uiState.update { it.copy(selectedScreen = SelectedScreen.WebSocketDetail(entry)) }
  }

  private fun navigateBack() {
    _uiState.update { it.copy(selectedScreen = SelectedScreen.List) }
  }

  // ========================================================================
  // Data Operations
  // ========================================================================

  private fun clearCurrentLogs() {
    when (_uiState.value.selectedTab) {
      NetworkInspectorTab.API -> {
        clearNetworkLogsUseCase.clearHttpLogs()
        formattedBodyCache.evictAll()
      }
      NetworkInspectorTab.WEBVIEW -> clearNetworkLogsUseCase.clearWebViewLogs()
      NetworkInspectorTab.WEBSOCKET -> clearNetworkLogsUseCase.clearWebSocketLogs()
    }
  }

  private fun clearAllLogs() {
    clearNetworkLogsUseCase()
    formattedBodyCache.evictAll()
  }
}
