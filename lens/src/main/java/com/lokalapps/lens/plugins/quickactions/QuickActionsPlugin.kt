package com.lokalapps.lens.plugins.quickactions

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.api.QuickActionsProvider
import com.lokalapps.lens.plugins.quickactions.ui.QuickActionsScreen

/**
 * Quick Actions plugin for Lens.
 *
 * Provides one-tap shortcuts for common debugging operations.
 */
class QuickActionsPlugin(private val provider: QuickActionsProvider) : ComposableLensPlugin {

  override val id = "quick_actions"
  override val name = "Quick Actions"
  override val icon = R.drawable.ic_lens_quick_actions
  override val description = "Debugging shortcuts"
  override val priority = 60

  override fun onInitialize(context: Context) {
    // No special initialization needed
  }

  @Composable
  override fun Content() {
    QuickActionsScreen(provider)
  }
}
