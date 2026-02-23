package com.behtar.lens.internal.presentation.network

import androidx.lifecycle.ViewModel
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.data.model.WebViewLogEntry
import com.behtar.lens.internal.domain.usecase.network.ClearNetworkLogsUseCase
import com.behtar.lens.internal.domain.usecase.network.GetNetworkLogsUseCase
import com.behtar.lens.internal.domain.usecase.network.GetWebSocketLogsUseCase
import com.behtar.lens.internal.domain.usecase.network.GetWebViewLogsUseCase
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
    _uiState.update { it.copy(selectedScreen = SelectedScreen.HttpDetail(entry)) }
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
      NetworkInspectorTab.API -> clearNetworkLogsUseCase.clearHttpLogs()
      NetworkInspectorTab.WEBVIEW -> clearNetworkLogsUseCase.clearWebViewLogs()
      NetworkInspectorTab.WEBSOCKET -> clearNetworkLogsUseCase.clearWebSocketLogs()
    }
  }

  private fun clearAllLogs() {
    clearNetworkLogsUseCase()
  }
}
