@file:OptIn(com.lokalapps.lens.api.LensExperimental::class)

package com.lokalapps.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.api.DefaultHeaderRedactor
import com.lokalapps.lens.api.HeaderRedactor
import com.lokalapps.lens.internal.di.LensServiceLocator
import com.lokalapps.lens.internal.interceptors.LensNetworkInterceptor
import com.lokalapps.lens.internal.presentation.network.NetworkInspectorScreen
import okhttp3.Interceptor

/**
 * Network Inspector plugin for Lens.
 *
 * Provides network request/response inspection for:
 * - HTTP/HTTPS requests via OkHttp interceptor
 * - WebView network traffic
 * - WebSocket connections and messages
 *
 * ## Architecture:
 * - Uses [LensServiceLocator] for repository access
 * - MVI pattern for UI state management
 *
 * ## Usage:
 * The interceptor should be obtained from [Lens.getNetworkInterceptor()].
 */
class NetworkPlugin(private val headerRedactor: HeaderRedactor = DefaultHeaderRedactor()) :
    ComposableLensPlugin {

  override val id = "network"
  override val name = "Network"
  override val icon = R.drawable.ic_lens_network
  override val description = "Inspect HTTP requests, WebView, and WebSocket traffic"
  override val priority = 100

  private var context: Context? = null

  /**
   * OkHttp interceptor that captures network traffic. Lazily initialized when [onInitialize] is
   * called.
   *
   * Add this to your OkHttpClient to enable network logging.
   */
  internal lateinit var interceptor: Interceptor
    private set

  override fun onInitialize(context: Context) {
    this.context = context.applicationContext

    // Initialize the network interceptor with header redaction
    interceptor = LensNetworkInterceptor(headerRedactor)

    // Clear any stale logs from previous sessions
    try {
      LensServiceLocator.networkLogRepository.clear()
      LensServiceLocator.webSocketLogRepository.clear()
      LensServiceLocator.webViewLogRepository.clear()
    } catch (e: Exception) {
      timber.log.Timber.w(e, "NetworkPlugin: Could not clear logs during init")
    }
  }

  @Composable
  override fun Content() {
    NetworkInspectorScreen()
  }
}
