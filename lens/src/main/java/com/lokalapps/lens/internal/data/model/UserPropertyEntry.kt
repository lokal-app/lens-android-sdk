package com.lokalapps.lens.internal.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a user property update captured by Lens.
 *
 * User properties are attributes set on analytics SDKs for user segmentation and targeting (e.g.,
 * user_id, subscription_status, language).
 *
 * @property id Unique identifier for this log entry
 * @property timestamp When the property was set (epoch millis)
 * @property userId The user ID being set (null for anonymous)
 * @property properties User properties as key-value pairs
 * @property destinations Set of target SDKs: "FIREBASE", "MOENGAGE", etc.
 */
data class UserPropertyEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String?,
    val properties: Map<String, Any?>,
    val destinations: Set<String>
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

  /** Number of properties in this update. */
  val propertyCount: Int
    get() = properties.size

  /** Short description for list display. */
  val summary: String
    get() = buildString {
      if (userId != null) {
        append("User: $userId")
      } else {
        append("Anonymous")
      }
      if (propertyCount > 0) {
        append(" ($propertyCount properties)")
      }
    }

  companion object {
    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
  }
}
