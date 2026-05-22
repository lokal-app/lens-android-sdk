package com.lokalapps.lens.plugins.featureflags

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.api.FeatureFlagProvider
import com.lokalapps.lens.plugins.featureflags.ui.FeatureFlagsScreen

/**
 * Feature Flags editor plugin for Lens.
 *
 * Allows toggling feature flags at runtime for testing.
 */
class FeatureFlagsPlugin(private val provider: FeatureFlagProvider) : ComposableLensPlugin {

  override val id = "feature_flags"
  override val name = "Feature Flags"
  override val icon = R.drawable.ic_lens_feature_flags
  override val description = "Toggle feature flags"
  override val priority = 65

  override fun onInitialize(context: Context) {
    // No special initialization needed
  }

  @Composable
  override fun Content() {
    FeatureFlagsScreen(provider)
  }
}
