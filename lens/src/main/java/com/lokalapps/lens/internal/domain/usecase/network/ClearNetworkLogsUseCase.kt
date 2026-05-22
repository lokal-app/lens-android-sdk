package com.lokalapps.lens.internal.domain.usecase.network

import com.lokalapps.lens.internal.data.repository.NetworkLogRepository
import com.lokalapps.lens.internal.data.repository.WebSocketLogRepository
import com.lokalapps.lens.internal.data.repository.WebViewLogRepository

/**
 * Use case for clearing network logs.
 *
 * Clears logs from all network-related repositories:
 * - HTTP/HTTPS logs
 * - WebSocket logs
 * - WebView logs
 */
class ClearNetworkLogsUseCase(
    private val networkLogRepository: NetworkLogRepository,
    private val webSocketLogRepository: WebSocketLogRepository,
    private val webViewLogRepository: WebViewLogRepository
) {
  /** Clears all network logs. */
  operator fun invoke() {
    networkLogRepository.clear()
    webSocketLogRepository.clear()
    webViewLogRepository.clear()
  }

  /** Clears only HTTP logs. */
  fun clearHttpLogs() {
    networkLogRepository.clear()
  }

  /** Clears only WebSocket logs. */
  fun clearWebSocketLogs() {
    webSocketLogRepository.clear()
  }

  /** Clears only WebView logs. */
  fun clearWebViewLogs() {
    webViewLogRepository.clear()
  }
}
