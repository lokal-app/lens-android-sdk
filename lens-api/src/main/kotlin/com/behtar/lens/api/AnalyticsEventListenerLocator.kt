package com.behtar.lens.api

/**
 * Service locator for analytics event listener.
 *
 * Lens registers its listener here during initialization. The analytics module checks this locator
 * and notifies the listener for every event.
 *
 * ## Why Service Locator?
 * This pattern is used because:
 * 1. The analytics module cannot depend on lens (would create circular dependency)
 * 2. Devtools needs to intercept events from analytics
 * 3. Using a singleton locator allows loose coupling between modules
 *
 * ## Thread Safety
 * The listener property is marked @Volatile for thread-safe reads/writes. However, the listener
 * implementation itself must also be thread-safe.
 *
 * ## Usage
 *
 * ```kotlin
 * // In Lens initialization
 * AnalyticsEventListenerLocator.listener = myListener
 *
 * // In analytics (CompositeAnalyticsRepository)
 * AnalyticsEventListenerLocator.listener?.onEvent(...)
 *
 * // When Lens is disabled
 * AnalyticsEventListenerLocator.listener = null
 * ```
 */
object AnalyticsEventListenerLocator {

  /**
   * The currently registered listener, or null if none.
   *
   * Lens sets this to its listener implementation during initialization. Setting to null disables
   * event capture.
   */
  @Volatile var listener: AnalyticsEventListener? = null
}
