package com.lokalapps.lens.api

import androidx.compose.runtime.Composable

/**
 * A [LensPlugin] that renders its UI using Jetpack Compose.
 *
 * This is the most common plugin type for modern Android apps. The [Content] composable is
 * displayed when the user selects this plugin from the Lens dashboard.
 *
 * ## Example:
 * ```kotlin
 * class NetworkPlugin : ComposableLensPlugin {
 *     override val id = "network"
 *     override val name = "Network Inspector"
 *     override val icon = R.drawable.ic_network
 *     override val description = "Inspect HTTP requests"
 *
 *     @Composable
 *     override fun Content() {
 *         NetworkInspectorScreen()
 *     }
 * }
 * ```
 *
 * @see ViewLensPlugin for Android View-based plugins
 */
interface ComposableLensPlugin : LensPlugin {

  /**
   * Composable content for the plugin's UI.
   *
   * This is displayed when the user selects this plugin from the dashboard. The content should fill
   * the available space and handle its own scrolling if needed.
   */
  @Composable fun Content()
}
