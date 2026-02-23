package com.behtar.lens.plugins.environment

import android.content.Context
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.api.EnvironmentProvider
import com.behtar.lens.plugins.environment.ui.EnvironmentSwitcherScreen

/**
 * Environment Switcher plugin for Lens.
 *
 * Allows switching between server environments (prod, staging, dev) at runtime.
 */
class EnvironmentPlugin(private val provider: EnvironmentProvider) : ComposableLensPlugin {

  override val id = "environment"
  override val name = "Environment"
  override val icon = R.drawable.ic_lens_environment
  override val description = "Switch server environments"
  override val priority = 70

  override fun onInitialize(context: Context) {
    // No special initialization needed
  }

  @Composable
  override fun Content() {
    EnvironmentSwitcherScreen(provider)
  }
}
