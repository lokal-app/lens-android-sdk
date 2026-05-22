package com.lokalapps.lens.internal.data.model

import java.util.UUID

/**
 * Represents a single network request/response pair in Lens.
 *
 * This data class captures all relevant information about an HTTP transaction for debugging
 * purposes, including headers, body, timing, and status.
 */
data class NetworkLogEntry(
    /** Unique identifier for this log entry */
    val id: String = UUID.randomUUID().toString(),

    /** HTTP method (GET, POST, PUT, DELETE, etc.) */
    val method: String,

    /** Full request URL */
    val url: String,

    /** Host portion of the URL (for grouping) */
    val host: String,

    /** Path portion of the URL */
    val path: String,

    /** Request headers */
    val requestHeaders: Map<String, String> = emptyMap(),

    /** Request body (if applicable) */
    val requestBody: String? = null,

    /** Content type of request body */
    val requestContentType: String? = null,

    /** Request body size in bytes */
    val requestBodySize: Long = 0,

    /** HTTP response status code */
    val responseCode: Int = 0,

    /** Response status message */
    val responseMessage: String? = null,

    /** Response headers */
    val responseHeaders: Map<String, String> = emptyMap(),

    /** Response body (if applicable) */
    val responseBody: String? = null,

    /** Content type of response body */
    val responseContentType: String? = null,

    /** Response body size in bytes */
    val responseBodySize: Long = 0,

    /** Timestamp when request was initiated (milliseconds) */
    val requestTimestamp: Long = System.currentTimeMillis(),

    /** Timestamp when response was received (milliseconds) */
    val responseTimestamp: Long = 0,

    /** Duration of the request in milliseconds */
    val durationMs: Long = 0,

    /** Whether the request was successful (2xx status) */
    val isSuccessful: Boolean = false,

    /** Whether the request is still in progress */
    val isInProgress: Boolean = true,

    /** Error message if the request failed */
    val errorMessage: String? = null,

    /** Protocol used (HTTP/1.1, HTTP/2, etc.) */
    val protocol: String? = null,

    /** TLS/SSL version if HTTPS */
    val tlsVersion: String? = null
) {
  /** Human-readable duration string. */
  val durationString: String
    get() =
        when {
          durationMs < 1000 -> "${durationMs}ms"
          else -> String.format("%.2fs", durationMs / 1000.0)
        }

  /** Human-readable size string for response. */
  val responseSizeString: String
    get() = formatSize(responseBodySize)

  /** Status indicator color hint. */
  val statusType: StatusType
    get() =
        when {
          isInProgress -> StatusType.PENDING
          errorMessage != null -> StatusType.ERROR
          responseCode in 200..299 -> StatusType.SUCCESS
          responseCode in 300..399 -> StatusType.REDIRECT
          responseCode in 400..499 -> StatusType.CLIENT_ERROR
          responseCode >= 500 -> StatusType.SERVER_ERROR
          else -> StatusType.UNKNOWN
        }

  private fun formatSize(bytes: Long): String =
      when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
      }

  /**
   * Generates a cURL command that can be used to reproduce this request.
   *
   * The generated command includes:
   * - HTTP method
   * - Full URL
   * - All request headers
   * - Request body (for POST/PUT/PATCH)
   * - --compressed flag if Accept-Encoding includes gzip
   *
   * @return A complete cURL command string ready to be copied and executed
   */
  fun toCurlCommand(): String {
    val builder = StringBuilder("curl")

    // Add method (skip for GET as it's default)
    if (method.uppercase() != "GET") {
      builder.append(" -X ${method.uppercase()}")
    }

    // Add URL (quoted to handle special characters)
    builder.append(" \\\n  '${url}'")

    // Add headers
    requestHeaders.forEach { (key, value) ->
      // Escape single quotes in header values
      val escapedValue = value.replace("'", "'\\''")
      builder.append(" \\\n  -H '$key: $escapedValue'")
    }

    // Add --compressed if gzip is accepted
    val acceptEncoding = requestHeaders["Accept-Encoding"] ?: requestHeaders["accept-encoding"]
    if (acceptEncoding?.contains("gzip", ignoreCase = true) == true) {
      builder.append(" \\\n  --compressed")
    }

    // Add body for methods that typically have one
    if (method.uppercase() in listOf("POST", "PUT", "PATCH", "DELETE") &&
        !requestBody.isNullOrBlank()) {
      // Escape single quotes in body
      val escapedBody = requestBody.replace("'", "'\\''")
      builder.append(" \\\n  -d '$escapedBody'")
    }

    return builder.toString()
  }

  /** Generates a single-line cURL command (easier for some terminals). */
  fun toCurlCommandOneLine(): String {
    return toCurlCommand().replace(" \\\n  ", " ")
  }

  enum class StatusType {
    PENDING,
    SUCCESS,
    REDIRECT,
    CLIENT_ERROR,
    SERVER_ERROR,
    ERROR,
    UNKNOWN
  }
}
