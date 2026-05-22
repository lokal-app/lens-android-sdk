package com.lokalapps.lens.internal.domain.usecase.network

import com.lokalapps.lens.internal.data.model.WebViewLogEntry
import com.lokalapps.lens.internal.data.repository.WebViewLogRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for retrieving WebView network logs.
 *
 * Provides access to the reactive stream of WebView requests.
 */
class GetWebViewLogsUseCase(private val repository: WebViewLogRepository) {
  /** Returns the flow of WebView logs. */
  operator fun invoke(): StateFlow<List<WebViewLogEntry>> = repository.logs

  /** Gets the count of captured requests. */
  fun getCount(): Int = repository.count
}
