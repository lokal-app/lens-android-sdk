@file:OptIn(com.lokalapps.lens.api.LensExperimental::class)

package com.lokalapps.lens.internal.interceptors

import com.lokalapps.lens.api.DefaultHeaderRedactor
import com.lokalapps.lens.api.HeaderRedactor
import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import com.lokalapps.lens.internal.di.LensServiceLocator
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import timber.log.Timber

/**
 * OkHttp Interceptor that captures network traffic for Lens inspection.
 *
 * This interceptor logs all HTTP requests and responses to [NetworkLogRepository], which can be
 * viewed in the Lens Network Inspector UI.
 *
 * ## Features:
 * - Captures request/response headers and bodies
 * - Measures request duration
 * - Handles streaming and large responses
 * - Thread-safe logging via singleton repository
 *
 * ## Usage:
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addInterceptor(Lens.getNetworkInterceptor())
 *     .build()
 * ```
 */
class LensNetworkInterceptor(private val headerRedactor: HeaderRedactor = DefaultHeaderRedactor()) :
    Interceptor {

  private val repository
    get() = LensServiceLocator.networkLogRepository

  companion object {
    private const val TAG = "LensNetwork"

    /** Maximum body size to log (1 MB) */
    private const val MAX_BODY_SIZE = 1_000_000L

    /** Content types that are considered text and should be logged */
    private val TEXT_CONTENT_TYPES =
        listOf(
            "text/",
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-www-form-urlencoded")
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val entryId = UUID.randomUUID().toString()

    // Log the request
    val entry = createRequestEntry(entryId, request)
    repository.addEntry(entry)

    val startTime = System.nanoTime()

    return try {
      val response = chain.proceed(request)
      val endTime = System.nanoTime()
      val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

      // Log the response
      logResponse(entryId, response, durationMs)
    } catch (e: Exception) {
      val endTime = System.nanoTime()
      val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

      // Log the error
      logError(entryId, e, durationMs)
      throw e
    }
  }

  /** Creates a network log entry for the request. */
  private fun createRequestEntry(entryId: String, request: okhttp3.Request): NetworkLogEntry {
    val url = request.url.toString()
    val parsedUrl =
        try {
          URL(url)
        } catch (e: Exception) {
          null
        }

    val requestBody = request.body
    val (bodyContent, bodySize) =
        if (requestBody != null && isTextContent(requestBody.contentType()?.toString())) {
          try {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            val content = buffer.readString(StandardCharsets.UTF_8)
            Pair(content.take(MAX_BODY_SIZE.toInt()), requestBody.contentLength().coerceAtLeast(0))
          } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to read request body")
            Pair(null, requestBody.contentLength().coerceAtLeast(0))
          }
        } else {
          Pair(null, requestBody?.contentLength()?.coerceAtLeast(0) ?: 0)
        }

    return NetworkLogEntry(
        id = entryId,
        method = request.method,
        url = url,
        host = parsedUrl?.host ?: "unknown",
        path = parsedUrl?.path ?: url,
        requestHeaders = headersToMap(request.headers),
        requestBody = bodyContent,
        requestContentType = requestBody?.contentType()?.toString(),
        requestBodySize = bodySize,
        requestTimestamp = System.currentTimeMillis(),
        isInProgress = true)
  }

  /** Updates the log entry with response data. */
  private fun logResponse(entryId: String, response: Response, durationMs: Long): Response {
    val responseBody = response.body
    val contentType = responseBody?.contentType()

    // Read response body if it's text content and within size limit
    val contentLength = responseBody?.contentLength() ?: -1L
    val isTooLarge = contentLength > MAX_BODY_SIZE
    val (bodyContent, bodySize, newResponse) =
        if (responseBody != null && isTextContent(contentType?.toString()) && !isTooLarge) {
          try {
            val source = responseBody.source()
            // Only buffer up to MAX_BODY_SIZE to prevent OOM on large responses
            source.request(MAX_BODY_SIZE)
            val buffer = source.buffer.clone()
            val content = buffer.readString(contentType?.charset() ?: StandardCharsets.UTF_8)

            val truncatedContent = content.take(MAX_BODY_SIZE.toInt())
            val actualSize = if (contentLength >= 0) contentLength else content.length.toLong()

            Triple(
                truncatedContent,
                actualSize,
                response.newBuilder().body(content.toResponseBody(contentType)).build())
          } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to read response body")
            Triple(null, contentLength.coerceAtLeast(0), response)
          }
        } else {
          Triple(null, responseBody?.contentLength()?.coerceAtLeast(0) ?: 0, response)
        }

    repository.updateEntry(entryId) { entry ->
      entry.copy(
          responseCode = response.code,
          responseMessage = response.message,
          responseHeaders = headersToMap(response.headers),
          responseBody = bodyContent,
          responseContentType = contentType?.toString(),
          responseBodySize = bodySize,
          responseTimestamp = System.currentTimeMillis(),
          durationMs = durationMs,
          isSuccessful = response.isSuccessful,
          isInProgress = false,
          protocol = response.protocol.toString(),
          tlsVersion = response.handshake?.tlsVersion?.javaName)
    }

    return newResponse
  }

  /** Updates the log entry with error information. */
  private fun logError(entryId: String, error: Exception, durationMs: Long) {
    repository.updateEntry(entryId) { entry ->
      entry.copy(
          responseTimestamp = System.currentTimeMillis(),
          durationMs = durationMs,
          isSuccessful = false,
          isInProgress = false,
          errorMessage = error.message ?: error.javaClass.simpleName)
    }
  }

  /** Converts OkHttp Headers to a Map, redacting sensitive values. */
  private fun headersToMap(headers: Headers): Map<String, String> {
    return headers.names().associateWith { name ->
      if (headerRedactor.shouldRedact(name)) {
        "[REDACTED]"
      } else {
        headers.values(name).joinToString(", ")
      }
    }
  }

  /** Checks if the content type is text-based and should be logged. */
  private fun isTextContent(contentType: String?): Boolean {
    if (contentType == null) return false
    val lowerContentType = contentType.lowercase()
    return TEXT_CONTENT_TYPES.any { lowerContentType.contains(it) }
  }
}
