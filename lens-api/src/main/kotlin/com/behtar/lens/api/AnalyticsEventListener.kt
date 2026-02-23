package com.behtar.lens.api

/**
 * Listener interface for analytics event interception.
 *
 * Used by Lens to capture analytics events in real-time for debugging. The listener receives all
 * events before they are dispatched to SDKs.
 *
 * ## Usage
 * Lens registers its listener via [AnalyticsEventListenerLocator]:
 * ```kotlin
 * AnalyticsEventListenerLocator.listener = myListener
 * ```
 *
 * ## Thread Safety
 * Implementations should be thread-safe as events may be fired from multiple threads (main thread,
 * background workers, etc.).
 */
interface AnalyticsEventListener {

  /**
   * Called when an analytics event is sent.
   *
   * @param eventName The name of the event (e.g., "TAP_CARD", "VIDEO_START")
   * @param params Event parameters as key-value pairs (may be null or empty)
   * @param destinations Set of target SDKs: "FIREBASE", "MOENGAGE", "ADJUST", "CLARITY", "FACEBOOK"
   */
  fun onEvent(eventName: String, params: Map<String, Any?>?, destinations: Set<String>)

  /**
   * Called when user properties are updated.
   *
   * @param userId The user ID being set (may be null for anonymous users)
   * @param params User properties as key-value pairs
   * @param destinations Set of target SDKs for the properties
   */
  fun onUserProperty(userId: String?, params: Map<String, Any?>?, destinations: Set<String>)

  /**
   * Called when a revenue event is sent (typically to Facebook).
   *
   * @param amount Revenue amount (may be null)
   * @param eventName The event name
   * @param params Additional event parameters
   * @param destinations Set of target SDKs (typically just "FACEBOOK")
   */
  fun onRevenueEvent(
      amount: Double?,
      eventName: String,
      params: Map<String, Any?>?,
      destinations: Set<String>
  )
}
