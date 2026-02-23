package com.behtar.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.internal.presentation.cache.CacheManagerScreen
import timber.log.Timber

/**
 * Cache Manager plugin for Lens.
 *
 * Shows cache sizes and allows clearing various app caches:
 * - Image cache (Glide memory + disk)
 * - App internal cache
 * - WebView cache
 * - External storage cache
 *
 * Useful for:
 * - Debugging image loading issues
 * - Freeing up disk space during testing
 * - Verifying cache invalidation behavior
 */
class CacheManagerPlugin : ComposableLensPlugin {

  override val id = "cache_manager"
  override val name = "Cache"
  override val icon = R.drawable.ic_lens_cache
  override val description = "View and clear app caches"
  override val priority = 70

  override fun onInitialize(context: Context) {
    Timber.d("CacheManagerPlugin: Initialized")
  }

  override fun onEnabled() {
    Timber.d("CacheManagerPlugin: Enabled")
  }

  override fun onDisabled() {
    Timber.d("CacheManagerPlugin: Disabled")
  }

  @Composable
  override fun Content() {
    CacheManagerScreen()
  }
}
