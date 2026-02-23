package com.behtar.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.internal.di.LensServiceLocator
import com.behtar.lens.internal.interceptors.LensAnalyticsListener
import com.behtar.lens.internal.presentation.analytics.AnalyticsInspectorScreen
import timber.log.Timber

/**
 * Analytics Inspector plugin for Lens.
 *
 * Provides real-time analytics event and user property inspection:
 * - Logs all analytics events sent to Firebase, MoEngage, Adjust, Clarity, Facebook
 * - Shows user property updates
 * - Filterable by destination SDK
 * - Searchable by event name or parameters
 * - Detailed view for each event with all parameters
 *
 * ## Architecture:
 * - Uses listener pattern to intercept events from CompositeAnalyticsRepository
 * - [LensServiceLocator] for repository access
 * - MVI pattern for UI state management
 *
 * ## Usage:
 * Automatically captures all analytics events when the plugin is initialized. No additional setup
 * required in the app code.
 */
class AnalyticsPlugin : ComposableLensPlugin {

  override val id = "analytics"
  override val name = "Analytics"
  override val icon = R.drawable.ic_lens_analytics
  override val description = "Inspect analytics events and user properties in real-time"
  override val priority = 90 // After Network (100), before Exceptions (85)

  private var context: Context? = null

  override fun onInitialize(context: Context) {
    this.context = context.applicationContext

    // Install the analytics listener to start capturing events
    LensAnalyticsListener.install()

    // Clear any stale logs from previous sessions
    try {
      LensServiceLocator.analyticsLogRepository.clear()
      Timber.d("AnalyticsPlugin: Initialized and ready to capture events")
    } catch (e: Exception) {
      Timber.w(e, "AnalyticsPlugin: Could not clear logs during init")
    }
  }

  override fun onDisabled() {
    // DON'T uninstall the listener when Lens drawer is closed.
    // Unlike NetworkPlugin which keeps its interceptor active all the time,
    // the old implementation uninstalled the listener here, which caused events
    // fired while the drawer was closed to be permanently lost.
    //
    // Users expect ALL analytics events to be captured (like network requests),
    // not just events fired while actively viewing the Lens dashboard.
    //
    // The listener overhead is negligible (just adds events to in-memory list
    // with MAX_EVENTS=1000 cap), so keeping it active doesn't impact performance.
    Timber.d("AnalyticsPlugin: Drawer closed (listener remains active to capture all events)")
  }

  override fun onEnabled() {
    // Listener is already installed from onInitialize()
    // Just ensure it's still installed in case of edge cases
    LensAnalyticsListener.install() // No-op if already installed
    Timber.d("AnalyticsPlugin: Drawer opened")
  }

  @Composable
  override fun Content() {
    AnalyticsInspectorScreen()
  }
}
