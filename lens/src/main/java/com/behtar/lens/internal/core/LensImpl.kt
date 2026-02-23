package com.behtar.lens.internal.core

import android.app.Application
import android.content.Context
import android.content.Intent
import com.behtar.lens.api.LensApi
import com.behtar.lens.api.LensConfig
import com.behtar.lens.api.LensPlugin
import com.behtar.lens.internal.core.plugin.PluginRegistry
import com.behtar.lens.internal.notification.LensNotificationManager
import com.behtar.lens.internal.plugins.AnalyticsPlugin
import com.behtar.lens.internal.plugins.AppInfoPlugin
import com.behtar.lens.internal.plugins.CacheManagerPlugin
import com.behtar.lens.internal.plugins.DatabasePlugin
import com.behtar.lens.internal.plugins.DeepLinkPlugin
import com.behtar.lens.internal.plugins.ExceptionsPlugin
import com.behtar.lens.internal.plugins.GlobalSearchPlugin
import com.behtar.lens.internal.plugins.LogViewerPlugin
import com.behtar.lens.internal.plugins.NetworkPlugin
import com.behtar.lens.internal.plugins.performance.AnrDetector
import com.behtar.lens.internal.plugins.performance.PerformancePlugin
import com.behtar.lens.internal.presentation.bubble.LensBubbleInjector
import com.behtar.lens.internal.presentation.dashboard.LensDashboardActivity
import com.behtar.lens.plugins.environment.EnvironmentPlugin
import com.behtar.lens.plugins.featureflags.FeatureFlagsPlugin
import com.behtar.lens.plugins.preferences.PreferencesPlugin
import com.behtar.lens.plugins.quickactions.QuickActionsPlugin
import okhttp3.Interceptor
import timber.log.Timber

/**
 * Main implementation of Lens when enabled.
 *
 * Handles:
 * - Plugin registration and lifecycle
 * - In-app bubble injection for activation
 * - Dashboard opening/closing
 * - Network interceptor management
 */
internal class LensImpl(private val application: Application, private val config: LensConfig) :
    LensApi {

  private val pluginRegistry = PluginRegistry()
  private val networkPlugin = NetworkPlugin(config.headerRedactor)
  private var bubbleInjector: LensBubbleInjector? = null
  private var notificationManager: LensNotificationManager? = null
  private var anrDetector: AnrDetector? = null
  private var _isOpen = false

  /**
   * SharedPreferences for Lens configuration storage. Used to persist WebView URL overrides and
   * other runtime settings.
   */
  private val prefs by lazy { application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

  override val isEnabled: Boolean = true
  override val isOpen: Boolean
    get() = _isOpen

  override fun initialize() {
    Timber.d("Lens: Initializing...")

    // Initialize the service locator with application context
    com.behtar.lens.internal.di.LensServiceLocator.initialize(application)

    // Register built-in plugins
    registerBuiltInPlugins()

    // Start in-app bubble injector (no permission needed)
    startBubbleInjector()

    // Start notification if enabled
    if (config.showNotification) {
      startNotification()
    }

    // Start ANR detection
    anrDetector = AnrDetector().also { it.start() }

    Timber.d("Lens: Initialized with ${pluginRegistry.size} plugins")
  }

  /**
   * Starts the in-app bubble injector that shows a floating bubble on every screen.
   *
   * Unlike the old FloatingBubbleService, this approach:
   * - Requires NO permissions (no SYSTEM_ALERT_WINDOW)
   * - Works immediately without user action
   * - Is more performant (no separate service)
   * - Follows Activity lifecycle properly
   *
   * The bubble is injected into each Activity's DecorView, so it appears on top of all content
   * within that Activity.
   */
  private fun startBubbleInjector() {
    bubbleInjector =
        LensBubbleInjector(application) {
          Timber.d("Lens: Bubble clicked - opening dashboard")
          open()
        }
    bubbleInjector?.start()
  }

  /**
   * Starts the notification manager that shows a sticky notification with live request count and
   * error count.
   */
  private fun startNotification() {
    notificationManager = LensNotificationManager(application).also { it.start() }
  }

  /** Registers all built-in plugins. */
  private fun registerBuiltInPlugins() {
    // Core plugins (always registered)
    pluginRegistry.register(networkPlugin)
    pluginRegistry.register(GlobalSearchPlugin())
    pluginRegistry.register(AppInfoPlugin())
    pluginRegistry.register(AnalyticsPlugin())
    pluginRegistry.register(ExceptionsPlugin())
    pluginRegistry.register(DatabasePlugin())
    pluginRegistry.register(PreferencesPlugin())
    pluginRegistry.register(DeepLinkPlugin())
    pluginRegistry.register(LogViewerPlugin())
    pluginRegistry.register(CacheManagerPlugin())
    pluginRegistry.register(PerformancePlugin())

    // Optional plugins (based on providers)
    config.environmentProvider?.let { provider ->
      pluginRegistry.register(EnvironmentPlugin(provider))
      Timber.d("Lens: Environment plugin registered")
    }

    config.featureFlagProvider?.let { provider ->
      pluginRegistry.register(FeatureFlagsPlugin(provider))
      Timber.d("Lens: Feature flags plugin registered")
    }

    config.quickActionsProvider?.let { provider ->
      pluginRegistry.register(QuickActionsPlugin(provider))
      Timber.d("Lens: Quick actions plugin registered")
    }

    // Initialize all registered plugins
    pluginRegistry.getAll().forEach { plugin ->
      try {
        plugin.onInitialize(application)
      } catch (e: Exception) {
        Timber.e(e, "Lens: Failed to initialize plugin '${plugin.id}'")
      }
    }
  }

  override fun open() {
    if (_isOpen) {
      Timber.d("Lens: Already open")
      return
    }

    _isOpen = true
    Timber.d("Lens: Opening dashboard")

    // Notify plugins
    pluginRegistry.getAll().forEach { plugin ->
      try {
        plugin.onEnabled()
      } catch (e: Exception) {
        Timber.e(e, "Lens: Error in plugin '${plugin.id}' onEnabled()")
      }
    }

    // Launch dashboard activity
    val intent =
        Intent(application, LensDashboardActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    application.startActivity(intent)
  }

  override fun close() {
    if (!_isOpen) return

    _isOpen = false
    Timber.d("Lens: Closing dashboard")

    // Notify plugins
    pluginRegistry.getAll().forEach { plugin ->
      try {
        plugin.onDisabled()
      } catch (e: Exception) {
        Timber.e(e, "Lens: Error in plugin '${plugin.id}' onDisabled()")
      }
    }
  }

  override fun getNetworkInterceptor(): Interceptor {
    return networkPlugin.interceptor
  }

  override fun registerPlugin(plugin: LensPlugin) {
    pluginRegistry.register(plugin)

    // Initialize the newly registered plugin
    try {
      plugin.onInitialize(application)
    } catch (e: Exception) {
      Timber.e(e, "Lens: Failed to initialize custom plugin '${plugin.id}'")
    }
  }

  override fun getPlugin(id: String): LensPlugin? {
    return pluginRegistry.get(id)
  }

  override fun getPlugins(): List<LensPlugin> {
    return pluginRegistry.getAll()
  }

  // ======================== Generic Key-Value Settings Store ========================

  override fun getString(key: String, default: String?): String? {
    return prefs.getString(key, default)
  }

  override fun putString(key: String, value: String?) {
    prefs
        .edit()
        .apply {
          if (value != null) {
            putString(key, value)
          } else {
            remove(key)
          }
        }
        .apply()
  }

  override fun getBoolean(key: String, default: Boolean): Boolean {
    return prefs.getBoolean(key, default)
  }

  override fun putBoolean(key: String, value: Boolean) {
    prefs.edit().putBoolean(key, value).apply()
  }

  /** Called by the dashboard when closed by the user. */
  internal fun onDashboardClosed() {
    close()
  }

  companion object {
    private const val PREFS_NAME = "lens_config"
  }
}
