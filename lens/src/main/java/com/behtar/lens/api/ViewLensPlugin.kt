package com.behtar.lens.api

import android.content.Context
import android.view.View

/**
 * A [LensPlugin] that renders its UI using Android Views.
 *
 * Use this for plugins in React Native, Java, or legacy View-based codebases where Jetpack Compose
 * is not available.
 *
 * The [createView] method is called when the user selects this plugin from the Lens dashboard. The
 * returned View is displayed in a Compose wrapper via `AndroidView`.
 *
 * ## Example:
 * ```kotlin
 * class LegacyPlugin : ViewLensPlugin {
 *     override val id = "legacy"
 *     override val name = "Legacy Debug"
 *     override val icon = R.drawable.ic_legacy
 *     override val description = "Legacy debugging tool"
 *
 *     override fun createView(context: Context): View {
 *         return LinearLayout(context).apply {
 *             addView(TextView(context).apply { text = "Hello" })
 *         }
 *     }
 * }
 * ```
 *
 * @see ComposableLensPlugin for Jetpack Compose-based plugins
 */
@LensExperimental
interface ViewLensPlugin : LensPlugin {

  /**
   * Creates the Android View for this plugin's UI.
   *
   * Called each time the user navigates to this plugin in the dashboard. The returned View is
   * wrapped in a Compose `AndroidView` for rendering.
   *
   * @param context The Activity context
   * @return A View representing this plugin's UI
   */
  fun createView(context: Context): View
}
