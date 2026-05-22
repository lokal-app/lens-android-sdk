package com.lokalapps.lens.internal.interceptors

import com.lokalapps.lens.api.AnalyticsEventListener
import com.lokalapps.lens.api.AnalyticsEventListenerLocator
import com.lokalapps.lens.internal.data.model.AnalyticsLogEntry
import com.lokalapps.lens.internal.data.model.UserPropertyEntry
import com.lokalapps.lens.internal.data.repository.AnalyticsLogRepository
import com.lokalapps.lens.internal.di.LensServiceLocator
import timber.log.Timber

/**
 * Lens implementation of [AnalyticsEventListener].
 *
 * Captures all analytics events and user properties passing through [CompositeAnalyticsRepository]
 * and stores them in [AnalyticsLogRepository] for display in the Analytics Inspector UI.
 *
 * ## Lifecycle
 * - Installed when [AnalyticsPlugin] is initialized
 * - Uninstalled when Lens is disabled
 *
 * ## Thread Safety
 * This listener may be called from any thread. The underlying repository implementation handles
 * thread synchronization.
 *
 * ## Usage
 *
 * ```kotlin
 * // In AnalyticsPlugin.onInitialize()
 * LensAnalyticsListener.install(context)
 *
 * // In AnalyticsPlugin.onDisabled() or when Lens is disabled
 * LensAnalyticsListener.uninstall()
 * ```
 */
class LensAnalyticsListener private constructor(private val repository: AnalyticsLogRepository) :
    AnalyticsEventListener {

  override fun onEvent(eventName: String, params: Map<String, Any?>?, destinations: Set<String>) {
    Timber.d("LensAnalyticsListener: Received event '$eventName' with destinations: $destinations")
    val entry =
        AnalyticsLogEntry(
            eventName = eventName, params = params ?: emptyMap(), destinations = destinations)
    repository.logEvent(entry)
  }

  override fun onUserProperty(
      userId: String?,
      params: Map<String, Any?>?,
      destinations: Set<String>
  ) {
    val entry =
        UserPropertyEntry(
            userId = userId, properties = params ?: emptyMap(), destinations = destinations)
    repository.logUserProperty(entry)
  }

  override fun onRevenueEvent(
      amount: Double?,
      eventName: String,
      params: Map<String, Any?>?,
      destinations: Set<String>
  ) {
    val entry =
        AnalyticsLogEntry(
            eventName = eventName,
            params = params ?: emptyMap(),
            destinations = destinations,
            isRevenueEvent = true,
            revenueAmount = amount)
    repository.logEvent(entry)
  }

  companion object {
    private var instance: LensAnalyticsListener? = null

    /**
     * Installs the analytics listener.
     *
     * After calling this, all analytics events will be captured and stored in the repository for
     * display.
     */
    fun install() {
      if (instance != null) {
        Timber.d("LensAnalyticsListener: Already installed")
        return
      }

      try {
        val repository = LensServiceLocator.analyticsLogRepository
        instance = LensAnalyticsListener(repository)
        AnalyticsEventListenerLocator.listener = instance

        Timber.d("LensAnalyticsListener: Installed successfully")
      } catch (e: Exception) {
        Timber.e(e, "LensAnalyticsListener: Failed to install")
      }
    }

    /**
     * Uninstalls the analytics listener.
     *
     * After calling this, analytics events will no longer be captured.
     */
    fun uninstall() {
      AnalyticsEventListenerLocator.listener = null
      instance = null
      Timber.d("LensAnalyticsListener: Uninstalled")
    }

    /**
     * Wraps an existing listener to also notify Lens.
     *
     * Use this if there's already a listener registered that you don't want to replace.
     *
     * @param existingListener The existing listener to wrap
     * @return A listener that notifies both Lens and the existing listener
     */
    fun wrap(existingListener: AnalyticsEventListener?): AnalyticsEventListener {
      install()
      return if (existingListener != null && instance != null) {
        CompositeListener(instance!!, existingListener)
      } else {
        instance
            ?: object : AnalyticsEventListener {
              override fun onEvent(
                  eventName: String,
                  params: Map<String, Any?>?,
                  destinations: Set<String>
              ) {}

              override fun onUserProperty(
                  userId: String?,
                  params: Map<String, Any?>?,
                  destinations: Set<String>
              ) {}

              override fun onRevenueEvent(
                  amount: Double?,
                  eventName: String,
                  params: Map<String, Any?>?,
                  destinations: Set<String>
              ) {}
            }
      }
    }
  }

  /** Composite listener that notifies multiple listeners. */
  private class CompositeListener(
      private val primary: AnalyticsEventListener,
      private val secondary: AnalyticsEventListener
  ) : AnalyticsEventListener {
    override fun onEvent(eventName: String, params: Map<String, Any?>?, destinations: Set<String>) {
      primary.onEvent(eventName, params, destinations)
      secondary.onEvent(eventName, params, destinations)
    }

    override fun onUserProperty(
        userId: String?,
        params: Map<String, Any?>?,
        destinations: Set<String>
    ) {
      primary.onUserProperty(userId, params, destinations)
      secondary.onUserProperty(userId, params, destinations)
    }

    override fun onRevenueEvent(
        amount: Double?,
        eventName: String,
        params: Map<String, Any?>?,
        destinations: Set<String>
    ) {
      primary.onRevenueEvent(amount, eventName, params, destinations)
      secondary.onRevenueEvent(amount, eventName, params, destinations)
    }
  }
}
