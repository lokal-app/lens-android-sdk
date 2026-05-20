package com.behtar.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.behtar.lens.R
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.api.DeepLinkProvider
import com.behtar.lens.internal.presentation.deeplink.DeepLinkTesterScreen
import timber.log.Timber

/**
 * Deep Link Tester plugin for Lens.
 *
 * Allows testing deep links without leaving the app:
 * - Enter any deep link URL
 * - Quick access to app-specific links via [DeepLinkProvider] (optional)
 * - History of recently tested links
 */
class DeepLinkPlugin(private val provider: DeepLinkProvider? = null) : ComposableLensPlugin {

  override val id = "deep_link"
  override val name = "Deep Links"
  override val icon = R.drawable.ic_lens_deep_link
  override val description = "Test deep links without leaving the app"
  override val priority = 75

  override fun onInitialize(context: Context) {
    Timber.d("DeepLinkPlugin: Initialized")
  }

  override fun onEnabled() {
    Timber.d("DeepLinkPlugin: Enabled")
  }

  override fun onDisabled() {
    Timber.d("DeepLinkPlugin: Disabled")
  }

  @Composable
  override fun Content() {
    DeepLinkTesterScreen(provider = provider)
  }
}
