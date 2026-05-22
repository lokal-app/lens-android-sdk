package com.lokalapps.lens.api

import android.content.Context

/**
 * Base interface for Lens plugins.
 *
 * A plugin represents a distinct debugging feature that can be accessed from the Lens dashboard.
 * This base interface defines plugin metadata and lifecycle hooks — it does **not** require Compose
 * or Android Views.
 *
 * ## Plugin Types
 *
 * Choose the sub-interface that matches your UI framework:
 * - [ComposableLensPlugin] — For Jetpack Compose UI (most common)
 * - [ViewLensPlugin] — For Android Views (React Native, Java, legacy code)
 *
 * ## Example:
 * ```kotlin
 * class MyPlugin : ComposableLensPlugin {
 *     override val id = "my_plugin"
 *     override val name = "My Plugin"
 *     override val icon = R.drawable.ic_custom
 *     override val description = "Custom debugging tool"
 *
 *     @Composable
 *     override fun Content() {
 *         Text("Hello from my plugin!")
 *     }
 * }
 * ```
 *
 * **Registration:**
 *
 * ```kotlin
 * Lens.registerPlugin(MyPlugin())
 * ```
 */
interface LensPlugin {

  /**
   * Unique identifier for the plugin. Used to distinguish plugins in the registry. Example:
   * "network", "preferences", "custom_logging"
   */
  val id: String

  /**
   * Display name shown in the dashboard. Should be human-readable and concise. Example: "Network
   * Inspector", "Shared Preferences"
   */
  val name: String

  /**
   * Icon resource ID for the plugin. Displayed in the dashboard alongside the name. Example:
   * R.drawable.ic_lens_network
   */
  val icon: Int

  /**
   * Brief description of what the plugin does. Shown in the dashboard to help users understand the
   * plugin's purpose. Example: "Inspect all HTTP requests and responses"
   */
  val description: String

  /**
   * Priority for ordering in the dashboard. Higher values = shown first. Default is 0. Built-in
   * plugins use values 80-100.
   *
   * Recommended ranges:
   * - 100: Critical debugging tools (Network)
   * - 80-99: Important tools (Preferences, Exceptions)
   * - 50-79: Standard tools (Environment, Feature Flags)
   * - 0-49: Custom/app-specific tools
   */
  val priority: Int
    get() = 0

  /**
   * Called when Lens is installed/initialized. Use this to set up resources, start background
   * processes, etc.
   *
   * @param context Application context
   */
  fun onInitialize(context: Context) {}

  /** Called when Lens dashboard is opened. Use this to refresh data or start monitoring. */
  fun onEnabled() {}

  /** Called when Lens dashboard is closed. Use this to pause monitoring or release resources. */
  fun onDisabled() {}

  /** Called when Lens is being destroyed. Use this for cleanup. */
  fun onDestroy() {}
}
