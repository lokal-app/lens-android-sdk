package com.behtar.lens.internal.data.repository

import com.behtar.lens.internal.data.model.WebViewLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [WebViewLogRepository].
 *
 * WebView requests don't go through OkHttp, so they're captured separately and stored here. The
 * Network Inspector UI combines all repositories.
 */
class WebViewLogRepositoryImpl : WebViewLogRepository {

  private companion object {
    const val MAX_ENTRIES = 200
  }

  private val _logs = MutableStateFlow<List<WebViewLogEntry>>(emptyList())

  override val logs: StateFlow<List<WebViewLogEntry>> = _logs.asStateFlow()

  override val count: Int
    get() = _logs.value.size

  override fun logRequest(
      url: String,
      method: String,
      headers: Map<String, String>,
      isMainFrame: Boolean
  ): String {
    val entry =
        WebViewLogEntry.fromRequest(
            url = url, method = method, headers = headers, isMainFrame = isMainFrame)
    addEntry(entry)
    return entry.id
  }

  override fun updateWithResponse(id: String, statusCode: Int, mimeType: String?) {
    val current = _logs.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      val entry = current[index]
      val responseTime = System.currentTimeMillis()
      val duration = responseTime - entry.timestamp
      current[index] =
          entry.copy(
              statusCode = statusCode,
              mimeType = mimeType,
              isCompleted = true,
              responseTimestamp = responseTime,
              durationMs = duration)
      _logs.value = current
    }
  }

  override fun updateWithError(id: String, errorMessage: String) {
    val current = _logs.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      val entry = current[index]
      val responseTime = System.currentTimeMillis()
      val duration = responseTime - entry.timestamp
      current[index] =
          entry.copy(
              errorMessage = errorMessage,
              isCompleted = true,
              responseTimestamp = responseTime,
              durationMs = duration)
      _logs.value = current
    }
  }

  override fun logCompleteRequest(
      url: String,
      method: String,
      headers: Map<String, String>,
      isMainFrame: Boolean,
      statusCode: Int?,
      mimeType: String?,
      errorMessage: String?
  ) {
    val entry =
        WebViewLogEntry(
            url = url,
            method = method,
            requestHeaders = headers,
            isMainFrame = isMainFrame,
            statusCode = statusCode,
            mimeType = mimeType,
            errorMessage = errorMessage,
            isCompleted = true)
    addEntry(entry)
  }

  private fun addEntry(entry: WebViewLogEntry) {
    val current = _logs.value.toMutableList()
    current.add(0, entry) // Add to beginning (newest first)

    // Trim if exceeds max
    if (current.size > MAX_ENTRIES) {
      _logs.value = current.take(MAX_ENTRIES)
    } else {
      _logs.value = current
    }
  }

  override fun clear() {
    _logs.value = emptyList()
  }
}
