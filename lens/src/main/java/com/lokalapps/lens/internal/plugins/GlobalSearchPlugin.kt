package com.lokalapps.lens.internal.plugins

import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.internal.presentation.search.GlobalSearchScreen

/**
 * Global Search plugin for Lens.
 *
 * Provides cross-plugin search across all log types:
 * - Network requests (URL, method, request/response bodies)
 * - Exceptions (class name, message, stack trace)
 * - Analytics events (event name, parameters)
 *
 * Registered at priority 99 so it appears near the top of the dashboard, just below the Network
 * plugin.
 */
class GlobalSearchPlugin : ComposableLensPlugin {

  override val id = "global_search"
  override val name = "Search"
  override val icon = R.drawable.ic_lens_search
  override val description = "Search across all logs (network, exceptions, analytics)"
  override val priority = 99

  @Composable
  override fun Content() {
    GlobalSearchScreen()
  }
}
