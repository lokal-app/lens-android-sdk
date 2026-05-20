package com.behtar.lens.internal.presentation.network

import android.util.LruCache
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.data.model.WebViewLogEntry
import com.behtar.lens.internal.presentation.LensViewModelFactory
import com.behtar.lens.internal.presentation.network.components.detail.HttpDetailScreen
import com.behtar.lens.internal.presentation.network.components.detail.WebSocketDetailScreen
import com.behtar.lens.internal.presentation.network.components.detail.WebViewDetailScreen
import com.behtar.lens.internal.presentation.network.components.list.ApiLogList
import com.behtar.lens.internal.presentation.network.components.list.WebSocketLogList
import com.behtar.lens.internal.presentation.network.components.list.WebViewLogList

/**
 * Network Inspector screen - the main entry point for the network debugging tool.
 *
 * ## Architecture:
 * This composable follows the MVI (Model-View-Intent) pattern:
 * - **Model**: [NetworkUiState] - immutable state
 * - **View**: This Composable that renders state
 * - **Intent**: [NetworkEvent] - user actions sent to ViewModel
 *
 * All user interactions flow through [NetworkViewModel.onEvent], which processes events and updates
 * the state accordingly.
 *
 * ## Data Sources:
 * - HTTP logs from [NetworkLogRepository] via [GetNetworkLogsUseCase]
 * - WebView logs from [WebViewLogRepository] via [GetWebViewLogsUseCase]
 * - WebSocket logs from [WebSocketLogRepository] via [GetWebSocketLogsUseCase]
 *
 * @param modifier Optional modifier for the root composable
 * @param viewModel ViewModel instance (auto-created via [LensViewModelFactory])
 */
@Composable
fun NetworkInspectorScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = viewModel(factory = LensViewModelFactory)
) {
  val uiState by viewModel.uiState.collectAsState()
  val httpLogs by viewModel.httpLogs.collectAsState()
  val webViewLogs by viewModel.webViewLogs.collectAsState()
  val webSocketLogs by viewModel.webSocketLogs.collectAsState()

  // AnnotatedString cache lives at screen scope — survives tab switches within the inspector,
  // cleared when the inspector is closed (composable leaves composition).
  // Storing AnnotatedString here (not ViewModel) keeps UI types out of the ViewModel layer.
  val highlightedBodyCache = remember { LruCache<String, AnnotatedString>(50) }

  NetworkInspectorContent(
      uiState = uiState,
      httpLogs = httpLogs,
      webViewLogs = webViewLogs,
      webSocketLogs = webSocketLogs,
      onEvent = viewModel::onEvent,
      formattedBodyCache = viewModel.formattedBodyCache,
      highlightedBodyCache = highlightedBodyCache,
      modifier = modifier)
}

/**
 * Stateless content composable for Network Inspector.
 *
 * This separation enables:
 * - Easy testing with mock state
 * - Preview support
 * - Clear separation of state management from UI
 *
 * @param uiState Current UI state
 * @param httpLogs List of HTTP log entries
 * @param webViewLogs List of WebView log entries
 * @param webSocketLogs List of WebSocket log entries
 * @param onEvent Callback for user events
 * @param modifier Optional modifier
 */
@Composable
private fun NetworkInspectorContent(
    uiState: NetworkUiState,
    httpLogs: List<NetworkLogEntry>,
    webViewLogs: List<WebViewLogEntry>,
    webSocketLogs: List<WebSocketLogEntry>,
    onEvent: (NetworkEvent) -> Unit,
    formattedBodyCache: LruCache<String, String>,
    highlightedBodyCache: LruCache<String, AnnotatedString>,
    modifier: Modifier = Modifier
) {
  // Navigation based on selected screen
  when (val screen = uiState.selectedScreen) {
    is SelectedScreen.HttpDetail -> {
      HttpDetailScreen(
          entry = screen.entry,
          selectedTab = uiState.selectedHttpDetailTab,
          onTabSelected = { onEvent(NetworkEvent.SelectHttpDetailTab(it)) },
          onBack = { onEvent(NetworkEvent.NavigateBack) },
          formattedBodyCache = formattedBodyCache,
          highlightedBodyCache = highlightedBodyCache,
          modifier = modifier)
    }
    is SelectedScreen.WebViewDetail -> {
      WebViewDetailScreen(
          entry = screen.entry,
          onBack = { onEvent(NetworkEvent.NavigateBack) },
          modifier = modifier)
    }
    is SelectedScreen.WebSocketDetail -> {
      WebSocketDetailScreen(
          entry = screen.entry,
          onBack = { onEvent(NetworkEvent.NavigateBack) },
          modifier = modifier)
    }
    SelectedScreen.List -> {
      NetworkInspectorListScreen(
          selectedTab = uiState.selectedTab,
          httpLogs = httpLogs,
          webViewLogs = webViewLogs,
          webSocketLogs = webSocketLogs,
          onEvent = onEvent,
          modifier = modifier)
    }
  }
}

/** Main list screen with tabs for different log types. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkInspectorListScreen(
    selectedTab: NetworkInspectorTab,
    httpLogs: List<NetworkLogEntry>,
    webViewLogs: List<WebViewLogEntry>,
    webSocketLogs: List<WebSocketLogEntry>,
    onEvent: (NetworkEvent) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    // Tab row
    PrimaryScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 8.dp) {
      NetworkInspectorTab.entries.forEach { tab ->
        val count =
            when (tab) {
              NetworkInspectorTab.API -> httpLogs.size
              NetworkInspectorTab.WEBVIEW -> webViewLogs.size
              NetworkInspectorTab.WEBSOCKET -> webSocketLogs.size
            }
        Tab(
            selected = selectedTab == tab,
            onClick = { onEvent(NetworkEvent.SelectTab(tab)) },
            text = { Text("${tab.title} ($count)") })
      }
    }

    // Tab content
    when (selectedTab) {
      NetworkInspectorTab.API ->
          ApiLogList(
              logs = httpLogs,
              onEntryClick = { onEvent(NetworkEvent.SelectHttpEntry(it)) },
              onClear = { onEvent(NetworkEvent.ClearCurrentLogs) })
      NetworkInspectorTab.WEBVIEW ->
          WebViewLogList(
              logs = webViewLogs,
              onEntryClick = { onEvent(NetworkEvent.SelectWebViewEntry(it)) },
              onClear = { onEvent(NetworkEvent.ClearCurrentLogs) })
      NetworkInspectorTab.WEBSOCKET ->
          WebSocketLogList(
              connections = webSocketLogs,
              onEntryClick = { onEvent(NetworkEvent.SelectWebSocketEntry(it)) },
              onClear = { onEvent(NetworkEvent.ClearCurrentLogs) })
    }
  }
}
