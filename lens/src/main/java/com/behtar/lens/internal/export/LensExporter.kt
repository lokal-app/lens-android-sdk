package com.behtar.lens.internal.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.di.LensServiceLocator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Exports Lens debug logs in standard formats for sharing.
 *
 * Supports two export formats:
 * - **HAR** (HTTP Archive 1.2): Industry standard for network logs. Can be imported into Chrome
 *   DevTools, Firefox, Charles Proxy, etc.
 * - **JSON**: All log types (network, exceptions, analytics) in a single JSON file.
 *
 * Files are written to the app's cache directory and shared via [FileProvider] so they work with
 * Android's share sheet without external storage permissions.
 */
internal object LensExporter {

  private val ISO_8601 =
      SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
        timeZone = TimeZone.getDefault()
      }

  /** Exports network logs as a HAR 1.2 file and opens the share sheet. */
  fun shareNetworkLogsAsHar(context: Context) {
    val logs = LensServiceLocator.networkLogRepository.logs.value
    if (logs.isEmpty()) {
      Timber.d("Lens: No network logs to export")
      return
    }

    val har = buildHar(logs)
    val file = writeToCache(context, "lens_network_${timestamp()}.har", har.toString(2))
    shareFile(context, file, "application/json", "Share Network Logs (HAR)")
  }

  /** Exports all logs (network, exceptions, analytics) as a JSON file and opens the share sheet. */
  fun shareAllLogsAsJson(context: Context) {
    val json = buildAllLogsJson()
    if (json.length() == 0) {
      Timber.d("Lens: No logs to export")
      return
    }

    val file = writeToCache(context, "lens_export_${timestamp()}.json", json.toString(2))
    shareFile(context, file, "application/json", "Share All Lens Logs")
  }

  /**
   * Builds a HAR 1.2 JSON object from network log entries.
   *
   * HAR spec: http://www.softwareishard.com/blog/har-12-spec/
   */
  internal fun buildHar(logs: List<NetworkLogEntry>): JSONObject {
    val entries = JSONArray()
    for (log in logs) {
      if (log.isInProgress) continue // Skip incomplete requests
      entries.put(buildHarEntry(log))
    }

    return JSONObject().apply {
      put(
          "log",
          JSONObject().apply {
            put("version", "1.2")
            put(
                "creator",
                JSONObject().apply {
                  put("name", "Lens Android SDK")
                  put("version", "1.0.0")
                })
            put("entries", entries)
          })
    }
  }

  private fun buildHarEntry(entry: NetworkLogEntry): JSONObject {
    return JSONObject().apply {
      put("startedDateTime", ISO_8601.format(Date(entry.requestTimestamp)))
      put("time", entry.durationMs)

      // Request
      put(
          "request",
          JSONObject().apply {
            put("method", entry.method)
            put("url", entry.url)
            put("httpVersion", entry.protocol ?: "HTTP/1.1")
            put("headers", headersToHarArray(entry.requestHeaders))
            put("queryString", JSONArray()) // Not parsed separately
            put("cookies", JSONArray())
            put("headersSize", -1)
            put("bodySize", entry.requestBodySize)
            if (!entry.requestBody.isNullOrEmpty()) {
              put(
                  "postData",
                  JSONObject().apply {
                    put("mimeType", entry.requestContentType ?: "application/octet-stream")
                    put("text", entry.requestBody)
                  })
            }
          })

      // Response
      put(
          "response",
          JSONObject().apply {
            put("status", entry.responseCode)
            put("statusText", entry.responseMessage ?: "")
            put("httpVersion", entry.protocol ?: "HTTP/1.1")
            put("headers", headersToHarArray(entry.responseHeaders))
            put("cookies", JSONArray())
            put(
                "content",
                JSONObject().apply {
                  put("size", entry.responseBodySize)
                  put("mimeType", entry.responseContentType ?: "application/octet-stream")
                  if (!entry.responseBody.isNullOrEmpty()) {
                    put("text", entry.responseBody)
                  }
                })
            put("redirectURL", "")
            put("headersSize", -1)
            put("bodySize", entry.responseBodySize)
          })

      // Timings
      put(
          "timings",
          JSONObject().apply {
            put("send", 0)
            put("wait", entry.durationMs)
            put("receive", 0)
          })

      // Cache
      put("cache", JSONObject())
    }
  }

  private fun headersToHarArray(headers: Map<String, String>): JSONArray {
    val array = JSONArray()
    headers.forEach { (name, value) ->
      array.put(
          JSONObject().apply {
            put("name", name)
            put("value", value)
          })
    }
    return array
  }

  /** Builds a combined JSON export with all log types. */
  internal fun buildAllLogsJson(): JSONObject {
    val networkLogs = LensServiceLocator.networkLogRepository.logs.value
    val exceptions = LensServiceLocator.exceptionLogRepository.exceptions.value
    val analyticsLogs = runBlocking {
      LensServiceLocator.analyticsLogRepository.getEvents().first()
    }

    val json = JSONObject()
    json.put("exportedAt", ISO_8601.format(Date()))
    json.put("version", "1.0.0")

    // Network logs
    if (networkLogs.isNotEmpty()) {
      val networkArray = JSONArray()
      networkLogs.forEach { entry -> networkArray.put(networkEntryToJson(entry)) }
      json.put("network", networkArray)
    }

    // Exceptions
    if (exceptions.isNotEmpty()) {
      val exceptionsArray = JSONArray()
      exceptions.forEach { entry -> exceptionsArray.put(entry.toJson()) }
      json.put("exceptions", exceptionsArray)
    }

    // Analytics
    if (analyticsLogs.isNotEmpty()) {
      val analyticsArray = JSONArray()
      analyticsLogs.forEach { entry -> analyticsArray.put(analyticsEntryToJson(entry)) }
      json.put("analytics", analyticsArray)
    }

    return json
  }

  private fun networkEntryToJson(entry: NetworkLogEntry): JSONObject {
    return JSONObject().apply {
      put("id", entry.id)
      put("method", entry.method)
      put("url", entry.url)
      put("host", entry.host)
      put("path", entry.path)
      put("requestHeaders", JSONObject(entry.requestHeaders))
      put("requestBody", entry.requestBody)
      put("requestContentType", entry.requestContentType)
      put("requestBodySize", entry.requestBodySize)
      put("responseCode", entry.responseCode)
      put("responseMessage", entry.responseMessage)
      put("responseHeaders", JSONObject(entry.responseHeaders))
      put("responseBody", entry.responseBody)
      put("responseContentType", entry.responseContentType)
      put("responseBodySize", entry.responseBodySize)
      put("requestTimestamp", entry.requestTimestamp)
      put("responseTimestamp", entry.responseTimestamp)
      put("durationMs", entry.durationMs)
      put("isSuccessful", entry.isSuccessful)
      put("isInProgress", entry.isInProgress)
      put("errorMessage", entry.errorMessage)
      put("protocol", entry.protocol)
      put("tlsVersion", entry.tlsVersion)
    }
  }

  private fun analyticsEntryToJson(entry: AnalyticsLogEntry): JSONObject {
    return JSONObject().apply {
      put("id", entry.id)
      put("timestamp", entry.timestamp)
      put("eventName", entry.eventName)
      put("params", JSONObject(entry.params.mapValues { it.value?.toString() }))
      put("destinations", JSONArray(entry.destinations.toList()))
      put("isRevenueEvent", entry.isRevenueEvent)
      if (entry.revenueAmount != null) {
        put("revenueAmount", entry.revenueAmount)
      }
    }
  }

  private fun writeToCache(context: Context, filename: String, content: String): File {
    val cacheDir = File(context.cacheDir, "lens_exports")
    cacheDir.mkdirs()

    // Clean up old exports (keep last 5)
    cacheDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }

    val file = File(cacheDir, filename)
    file.writeText(content)
    return file
  }

  private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.lens.fileprovider", file)

    val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = mimeType
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    context.startActivity(
        Intent.createChooser(intent, title).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
  }

  private fun timestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
  }
}
