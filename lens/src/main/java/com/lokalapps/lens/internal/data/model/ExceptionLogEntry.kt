package com.lokalapps.lens.internal.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a captured exception in the Lens exception logger.
 *
 * @property id Unique identifier for this exception entry
 * @property timestamp When the exception was captured
 * @property threadName Name of the thread where the exception occurred
 * @property exceptionClass Full class name of the exception
 * @property message Exception message
 * @property stackTrace Full stack trace as a string
 * @property isHandled Whether this was a handled or uncaught exception
 * @property additionalInfo Optional extra context about the exception
 */
data class ExceptionLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val threadName: String,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: String,
    val isHandled: Boolean = false,
    val additionalInfo: Map<String, String> = emptyMap()
) {
  /** Simple class name (without package). */
  val simpleClassName: String
    get() = exceptionClass.substringAfterLast('.')

  /** Formatted timestamp for display. */
  val formattedTimestamp: String
    get() {
      val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  /** Formatted date for detail view. */
  val formattedDate: String
    get() {
      val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  /** First line of stack trace (usually the most relevant location). */
  val topStackLine: String
    get() {
      val lines = stackTrace.lines()
      return lines.firstOrNull { it.trim().startsWith("at ") }?.trim()?.removePrefix("at ")
          ?: lines.firstOrNull()
          ?: "No stack trace available"
    }

  /** Serializes this entry to JSON for disk persistence. */
  fun toJson(): JSONObject {
    return JSONObject().apply {
      put("id", id)
      put("timestamp", timestamp)
      put("threadName", threadName)
      put("exceptionClass", exceptionClass)
      put("message", message ?: JSONObject.NULL)
      put("stackTrace", stackTrace)
      put("isHandled", isHandled)
      put("additionalInfo", JSONObject(additionalInfo))
    }
  }

  companion object {
    /** Creates an ExceptionLogEntry from a Throwable. */
    fun fromThrowable(
        throwable: Throwable,
        threadName: String = Thread.currentThread().name,
        isHandled: Boolean = false,
        additionalInfo: Map<String, String> = emptyMap()
    ): ExceptionLogEntry {
      return ExceptionLogEntry(
          threadName = threadName,
          exceptionClass = throwable::class.java.name,
          message = throwable.message,
          stackTrace = throwable.stackTraceToString(),
          isHandled = isHandled,
          additionalInfo = additionalInfo)
    }

    /** Deserializes an entry from JSON. */
    fun fromJson(json: JSONObject): ExceptionLogEntry {
      val additionalInfoJson = json.optJSONObject("additionalInfo")
      val additionalInfo = mutableMapOf<String, String>()
      additionalInfoJson?.keys()?.forEach { key ->
        additionalInfo[key] = additionalInfoJson.optString(key, "")
      }

      return ExceptionLogEntry(
          id = json.getString("id"),
          timestamp = json.getLong("timestamp"),
          threadName = json.getString("threadName"),
          exceptionClass = json.getString("exceptionClass"),
          message = json.optString("message", null).takeIf { it != "null" },
          stackTrace = json.getString("stackTrace"),
          isHandled = json.getBoolean("isHandled"),
          additionalInfo = additionalInfo)
    }

    /** Serializes a list of entries to JSON string. */
    fun listToJson(entries: List<ExceptionLogEntry>): String {
      val array = JSONArray()
      entries.forEach { array.put(it.toJson()) }
      return array.toString()
    }

    /** Deserializes a list of entries from JSON string. */
    fun listFromJson(jsonString: String): List<ExceptionLogEntry> {
      if (jsonString.isBlank()) return emptyList()
      return try {
        val array = JSONArray(jsonString)
        (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
      } catch (e: Exception) {
        emptyList()
      }
    }
  }
}
