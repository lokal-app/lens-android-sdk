package com.behtar.lens.api

import android.app.Application
import com.behtar.lens.internal.core.LensImpl
import com.behtar.lens.internal.core.NoOpLens
import okhttp3.Interceptor
import timber.log.Timber

/**
 * Lens SDK - Production-grade on-device debugging framework.
 *
 * Lens provides a floating bubble/dashboard for debugging Android apps with:
 * - **Network Inspector**: View all HTTP requests and responses
 * - **SharedPreferences**: View and edit SharedPreferences values
 * - **Exceptions**: View crash logs and ANRs
 * - **Environment Switcher**: Switch between server environments
 * - **Feature Flags**: Toggle feature flags at runtime
 * - **Quick Actions**: Custom debugging shortcuts
 *
 * ## Quick Start
 *
 * ```kotlin
 * // In Application.onCreate()
 * Lens.install(this) {
 *     enabled = BuildConfig.DEBUG || BuildConfig.INTERNAL_TOOLS_ENABLED
 *     remoteActivation { callback -> callback(true) }  // Optional: Remote kill switch
 *     activationGesture = ActivationGesture.FIVE_TAP
 *     shakeToOpenEnabled = true
 *
 *     // Optional: App-specific providers
 *     environments(MyEnvironmentProvider(this@MyApplication))
 *     featureFlags(MyFeatureFlagProvider(this@MyApplication))
 *     quickActions(MyQuickActionsProvider(this@MyApplication))
 * }
 * ```
 *
 * ## Network Interceptor
 *
 * Add the Lens interceptor to your OkHttp client to capture network traffic:
 * ```kotlin
 * val okHttpClient = OkHttpClient.Builder()
 *     .addInterceptor(Lens.getNetworkInterceptor())
 *     .build()
 * ```
 *
 * ## Activation Methods
 *
 * Lens can be activated by:
 * 1. **Tap Gesture**: Tap the screen 5 times (configurable)
 * 2. **Shake**: Shake the device (configurable)
 * 3. **Remote Activation**: Controlled via [RemoteActivationProvider] (e.g., Firebase Remote
 *    Config)
 * 4. **Programmatic**: Call [Lens.open()]
 *
 * ## Security
 * - Lens is completely disabled when `enabled = false` (zero overhead)
 * - [RemoteActivationProvider] provides a kill switch without app update
 * - Gesture/shake activation is non-discoverable by regular users
 *
 * ## Custom Plugins
 *
 * You can add custom debugging panels:
 * ```kotlin
 * Lens.registerPlugin(MyCustomPlugin())
 * ```
 *
 * @see LensConfig for configuration options
 * @see LensPlugin for creating custom plugins
 */
object Lens {

  private var implementation: LensApi = NoOpLens

  /**
   * Delegating interceptor that always uses the current implementation.
   *
   * This is crucial because OkHttpClient is typically built by DI frameworks before Lens.install()
   * is called. If we returned implementation.getNetworkInterceptor() directly, it would capture the
   * NoOpLens reference and never update.
   *
   * By using a delegating interceptor, we ensure that network requests are always forwarded to the
   * current implementation, even if it changes after OkHttpClient was built.
   */
  private val delegatingInterceptor = Interceptor { chain ->
    implementation.getNetworkInterceptor().intercept(chain)
  }

  /**
   * Whether Lens is currently enabled. Returns `false` if Lens was installed with `enabled =
   * false`.
   */
  val isEnabled: Boolean
    get() = implementation.isEnabled

  /** Whether the Lens dashboard is currently open. */
  val isOpen: Boolean
    get() = implementation.isOpen

  /**
   * Installs Lens with the given configuration.
   *
   * This should be called once in your Application's `onCreate()` method, before any other
   * initialization that uses the network interceptor.
   *
   * @param application Your Application instance
   * @param config Configuration DSL block
   * @sample
   *
   * ```kotlin
   * class MyApplication : Application() {
   *     override fun onCreate() {
   *         super.onCreate()
   *
   *         Lens.install(this) {
   *             enabled = BuildConfig.DEBUG
   *             activationGesture = ActivationGesture.FIVE_TAP
   *             shakeToOpenEnabled = true
   *         }
   *     }
   * }
   * ```
   */
  fun install(application: Application, config: LensConfig.Builder.() -> Unit) {
    val builder = LensConfig.Builder()
    builder.config()
    val configuration = builder.build()

    Timber.d("Lens: Installing with enabled=${configuration.enabled}")

    implementation =
        if (configuration.enabled) {
          LensImpl(application, configuration)
        } else {
          NoOpLens
        }

    implementation.initialize()
  }

  /**
   * Opens the Lens dashboard.
   *
   * This can be called programmatically to open Lens, for example from a debug menu or a secret
   * button.
   *
   * No-op if Lens is disabled or already open.
   */
  fun open() = implementation.open()

  /**
   * Closes the Lens dashboard.
   *
   * No-op if Lens is disabled or already closed.
   */
  fun close() = implementation.close()

  /**
   * Gets the network interceptor to add to OkHttp.
   *
   * When Lens is enabled, this captures all network traffic for inspection.
   *
   * When Lens is disabled, this passes requests through with zero overhead.
   *
   * **Important:** This returns a delegating interceptor that always checks the current Lens
   * implementation. This ensures network logging works even if OkHttpClient is built before
   * Lens.install() is called (which is common with dependency injection frameworks).
   *
   * @return OkHttp [Interceptor] for network logging
   * @sample
   *
   * ```kotlin
   * val okHttpClient = OkHttpClient.Builder()
   *     .addInterceptor(loggingInterceptor)
   *     .addInterceptor(Lens.getNetworkInterceptor())
   *     .build()
   * ```
   */
  fun getNetworkInterceptor(): Interceptor = delegatingInterceptor

  /**
   * Registers a custom plugin.
   *
   * Custom plugins allow you to add app-specific debugging panels to the Lens dashboard.
   *
   * @param plugin The plugin to register
   * @sample
   *
   * ```kotlin
   * class MyDebugPlugin : ComposableLensPlugin {
   *     override val id = "my_debug"
   *     override val name = "My Debug Panel"
   *     override val icon = R.drawable.ic_debug
   *     override val description = "Custom debugging tools"
   *
   *     @Composable
   *     override fun Content() {
   *         Text("Hello from my plugin!")
   *     }
   * }
   *
   * Lens.registerPlugin(MyDebugPlugin())
   * ```
   *
   * @see ComposableLensPlugin for Compose-based plugins
   * @see ViewLensPlugin for View-based plugins
   */
  fun registerPlugin(plugin: LensPlugin) = implementation.registerPlugin(plugin)

  /**
   * Gets a registered plugin by its ID.
   *
   * @param id The plugin's unique identifier
   * @return The plugin, or null if not found
   */
  fun getPlugin(id: String): LensPlugin? = implementation.getPlugin(id)

  /**
   * Gets all registered plugins.
   *
   * @return List of all registered plugins, sorted by priority (highest first)
   */
  fun getPlugins(): List<LensPlugin> = implementation.getPlugins()

  // ======================== Key-Value Settings Store ========================

  /**
   * Gets a string value from Lens runtime settings.
   *
   * This provides a generic key-value store that plugins and app code can use to persist debug
   * configuration. Values are stored in SharedPreferences and survive app restarts.
   *
   * When Lens is disabled (no-op), this always returns [default].
   *
   * @param key The setting key
   * @param default Default value if key is not set
   * @return The stored value, or [default]
   * @sample
   *
   * ```kotlin
   * val baseUrl = Lens.getString("webview_base_url") ?: BuildConfig.WEBVIEW_BASE_URL
   * ```
   */
  fun getString(key: String, default: String? = null): String? =
      implementation.getString(key, default)

  /**
   * Stores a string value in Lens runtime settings.
   *
   * Pass `null` to remove the key.
   *
   * @param key The setting key
   * @param value The value to store, or null to remove
   */
  fun putString(key: String, value: String?) = implementation.putString(key, value)

  /**
   * Gets a boolean value from Lens runtime settings.
   *
   * When Lens is disabled (no-op), this always returns [default].
   *
   * @param key The setting key
   * @param default Default value if key is not set
   * @return The stored value, or [default]
   */
  fun getBoolean(key: String, default: Boolean = false): Boolean =
      implementation.getBoolean(key, default)

  /**
   * Stores a boolean value in Lens runtime settings.
   *
   * @param key The setting key
   * @param value The value to store
   */
  fun putBoolean(key: String, value: Boolean) = implementation.putBoolean(key, value)

  // ======================== End Key-Value Settings Store ========================

  /**
   * Wraps a WebViewClient with Lens network logging.
   *
   * WebView requests don't go through OkHttp, so they need separate interception. Use this method
   * to wrap your WebViewClient.
   *
   * @param client The WebViewClient to wrap (can be null for default behavior)
   * @return A WebViewClient that logs to Lens Network Inspector
   * @sample
   *
   * ```kotlin
   * // In your Activity/Fragment
   * val myClient = MyWebViewClient()
   * webView.webViewClient = Lens.wrapWebViewClient(myClient)
   *
   * // Or without an existing client
   * webView.webViewClient = Lens.wrapWebViewClient(null)
   * ```
   */
  fun wrapWebViewClient(client: android.webkit.WebViewClient?): android.webkit.WebViewClient {
    return com.behtar.lens.internal.interceptors.LensWebViewClient.wrap(client)
  }

  /**
   * Creates a WebSocket listener wrapper that logs WebSocket events.
   *
   * Use this to wrap your WebSocket listener for logging.
   *
   * @param listener The original WebSocketListener to wrap
   * @return A WebSocketListener that logs to Lens
   * @sample
   *
   * ```kotlin
   * val myListener = MyWebSocketListener()
   * val client = OkHttpClient()
   * val request = Request.Builder().url("wss://example.com").build()
   * client.newWebSocket(request, Lens.wrapWebSocketListener(myListener))
   * ```
   */
  fun wrapWebSocketListener(listener: okhttp3.WebSocketListener): okhttp3.WebSocketListener {
    return com.behtar.lens.internal.interceptors.LensWebSocketListener.wrap(listener)
  }

  /**
   * Returns the current implementation.
   *
   * @suppress Internal API — do not use from app code.
   */
  internal fun getImplementation(): LensApi = implementation
}

/**
 * Internal API interface for Lens implementations.
 *
 * @suppress
 */
internal interface LensApi {
  val isEnabled: Boolean
  val isOpen: Boolean

  fun initialize()

  fun open()

  fun close()

  fun getNetworkInterceptor(): Interceptor

  fun registerPlugin(plugin: LensPlugin)

  fun getPlugin(id: String): LensPlugin?

  fun getPlugins(): List<LensPlugin>

  // Generic key-value settings store
  fun getString(key: String, default: String?): String?

  fun putString(key: String, value: String?)

  fun getBoolean(key: String, default: Boolean): Boolean

  fun putBoolean(key: String, value: Boolean)
}
