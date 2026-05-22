package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.WebViewLogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for WebView network logs.
 *
 * WebView requests don't go through OkHttp, so they're captured separately and stored here. The
 * Network Inspector UI combines all repositories.
 */
interface WebViewLogRepository {

  /** Observable flow of all captured WebView requests. Newest requests appear first. */
  val logs: StateFlow<List<WebViewLogEntry>>

  /** Count of captured WebView requests */
  val count: Int

  /**
   * Logs a new WebView request.
   *
   * @param url The request URL
   * @param method HTTP method
   * @param headers Request headers
   * @param isMainFrame Whether this is the main document
   * @return The ID of the created log entry (for updating later)
   */
  fun logRequest(
      url: String,
      method: String = "GET",
      headers: Map<String, String> = emptyMap(),
      isMainFrame: Boolean = false
  ): String

  /**
   * Updates an existing entry with response information.
   *
   * @param id The entry ID from [logRequest]
   * @param statusCode HTTP response status code
   * @param mimeType Response MIME type
   */
  fun updateWithResponse(id: String, statusCode: Int, mimeType: String? = null)

  /**
   * Updates an existing entry with error information.
   *
   * @param id The entry ID from [logRequest]
   * @param errorMessage Error description
   */
  fun updateWithError(id: String, errorMessage: String)

  /** Logs a completed request with all information. */
  fun logCompleteRequest(
      url: String,
      method: String = "GET",
      headers: Map<String, String> = emptyMap(),
      isMainFrame: Boolean = false,
      statusCode: Int? = null,
      mimeType: String? = null,
      errorMessage: String? = null
  )

  /** Clears all WebView logs. */
  fun clear()
}
