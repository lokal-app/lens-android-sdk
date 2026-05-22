package com.lokalapps.lens.internal.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a logged analytics event captured by Lens.
 *
 * Each entry contains the complete event data including:
 * - Event name and parameters
 * - Target SDK destinations (can be multiple for broadcast events)
 * - Timestamp for chronological ordering
 * - Revenue information (if applicable)
 *
 * @property id Unique identifier for this log entry
 * @property timestamp When the event was captured (epoch millis)
 * @property eventName The analytics event name (e.g., "TAP_CARD", "VIDEO_START")
 * @property params Event parameters as key-value pairs
 * @property destinations Set of target SDKs: "FIREBASE", "MOENGAGE", etc.
 * @property isRevenueEvent Whether this is a revenue/purchase event
 * @property revenueAmount Revenue amount for purchase events (null otherwise)
 */
data class AnalyticsLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventName: String,
    val params: Map<String, Any?>,
    val destinations: Set<String>,
    val isRevenueEvent: Boolean = false,
    val revenueAmount: Double? = null
) {
  /**
   * Destination for display - only shows for single-SDK events. Returns null for broadcast events
   * (no need to show - it's the default).
   */
  val displayDestination: String?
    get() = if (destinations.size == 1) destinations.first() else null

  /** Formatted timestamp for display (HH:mm:ss.SSS). */
  val formattedTime: String
    get() = TIME_FORMAT.format(Date(timestamp))

  /** Number of parameters in this event. */
  val paramCount: Int
    get() = params.size

  /** Short description of the event for list display. */
  val summary: String
    get() = buildString {
      append(eventName)
      if (paramCount > 0) {
        append(" ($paramCount params)")
      }
      if (isRevenueEvent && revenueAmount != null) {
        append(" ₹$revenueAmount")
      }
    }

  companion object {
    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
  }
}
