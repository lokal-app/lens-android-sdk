package com.lokalapps.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.internal.presentation.appinfo.AppInfoScreen
import timber.log.Timber

/**
 * App Info plugin for Lens.
 *
 * Provides a single dashboard showing all relevant app and user information:
 * - Build info (version, flavor, build type)
 * - User info (user ID, subscription status)
 * - Device info (model, OS, RAM)
 * - Session info
 * - Key remote config values
 *
 * Useful for quickly debugging user issues without navigating multiple screens.
 */
class AppInfoPlugin : ComposableLensPlugin {

  override val id = "app_info"
  override val name = "App Info"
  override val icon = R.drawable.ic_lens_app_info
  override val description = "View build, user, device, and session info"
  override val priority = 95 // High priority - often needed first

  private var context: Context? = null

  override fun onInitialize(context: Context) {
    this.context = context.applicationContext
    Timber.d("AppInfoPlugin: Initialized")
  }

  override fun onEnabled() {
    Timber.d("AppInfoPlugin: Enabled")
  }

  override fun onDisabled() {
    Timber.d("AppInfoPlugin: Disabled")
  }

  @Composable
  override fun Content() {
    AppInfoScreen()
  }
}
