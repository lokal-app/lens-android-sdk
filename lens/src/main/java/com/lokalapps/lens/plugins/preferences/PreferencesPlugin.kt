package com.lokalapps.lens.plugins.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.plugins.preferences.ui.PreferencesScreen

/**
 * SharedPreferences inspector plugin for Lens.
 *
 * Allows viewing SharedPreferences values at runtime by reading the XML files directly from the
 * app's `shared_prefs` directory.
 */
class PreferencesPlugin : ComposableLensPlugin {

  override val id = "preferences"
  override val name = "Preferences"
  override val icon = R.drawable.ic_lens_preferences
  override val description = "View SharedPreferences"
  override val priority = 90

  override fun onInitialize(context: Context) {
    // No initialization needed - we read SharedPreferences on-demand
  }

  @Composable
  override fun Content() {
    PreferencesScreen()
  }
}
