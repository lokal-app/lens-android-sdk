package com.lokalapps.lens.internal.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a captured WebView network request.
 *
 * WebView requests don't go through OkHttp, so we capture them separately using WebViewClient
 * callbacks and JavaScript injection.
 *
 * @property id Unique identifier for this entry
 * @property timestamp When the request was initiated
 * @property url Full URL of the request
 * @property method HTTP method (GET, POST, etc.) - may be "GET" by default for main frame loads
 * @property requestHeaders Request headers (limited availability in WebView)
 * @property statusCode HTTP response status code (if available)
 * @property mimeType Response MIME type (if available)
 * @property isMainFrame Whether this is the main document or a sub-resource
 * @property errorMessage Error message if the request failed
 */
data class WebViewLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val url: String,
    val method: String = "GET",
    val requestHeaders: Map<String, String> = emptyMap(),
    val statusCode: Int? = null,
    val mimeType: String? = null,
    val isMainFrame: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    /** Timestamp when response was received (milliseconds) */
    val responseTimestamp: Long = 0,
    /** Duration of the request in milliseconds */
    val durationMs: Long = 0
) {
  /** Host portion of the URL. */
  val host: String
    get() =
        try {
          java.net.URL(url).host
        } catch (e: Exception) {
          url.substringAfter("://").substringBefore("/")
        }

  /** Path portion of the URL. */
  val path: String
    get() =
        try {
          val parsed = java.net.URL(url)
          parsed.path.ifEmpty { "/" }
        } catch (e: Exception) {
          url.substringAfter(host).substringBefore("?").ifEmpty { "/" }
        }

  /** Formatted timestamp for display. */
  val formattedTimestamp: String
    get() {
      val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  /** Human-readable duration string. Returns null if request is not completed or duration is 0. */
  val durationString: String?
    get() =
        when {
          !isCompleted || durationMs == 0L -> null
          durationMs < 1000 -> "${durationMs}ms"
          else -> String.format("%.2fs", durationMs / 1000.0)
        }

  /** Status type based on response code. */
  val statusType: StatusType
    get() =
        when {
          statusCode == null -> StatusType.PENDING
          errorMessage != null -> StatusType.ERROR
          statusCode in 200..299 -> StatusType.SUCCESS
          statusCode in 300..399 -> StatusType.REDIRECT
          statusCode in 400..499 -> StatusType.CLIENT_ERROR
          statusCode >= 500 -> StatusType.SERVER_ERROR
          else -> StatusType.UNKNOWN
        }

  enum class StatusType {
    SUCCESS,
    REDIRECT,
    CLIENT_ERROR,
    SERVER_ERROR,
    ERROR,
    PENDING,
    UNKNOWN
  }

  companion object {
    /** Creates a WebViewLogEntry from a WebResourceRequest. */
    fun fromRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        isMainFrame: Boolean = false
    ): WebViewLogEntry {
      return WebViewLogEntry(
          url = url, method = method, requestHeaders = headers, isMainFrame = isMainFrame)
    }
  }
}
