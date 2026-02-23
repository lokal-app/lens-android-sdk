package com.behtar.lens.api

import android.app.Application
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.WebSocketListener

/**
 * No-op Lens stub for release builds. All methods are empty - zero overhead.
 *
 * This module is included in release builds to provide API compatibility without bundling the full
 * Lens implementation (~1-2 MB savings).
 */
object Lens {

  /** Always false in no-op implementation */
  val isEnabled: Boolean = false

  /** Always false in no-op implementation */
  val isOpen: Boolean = false

  fun install(application: Application, config: LensConfig.Builder.() -> Unit) {
    // No-op
  }

  fun open() {
    // No-op
  }

  fun close() {
    // No-op
  }

  fun getNetworkInterceptor(): Interceptor = NoOpInterceptor

  fun registerPlugin(plugin: LensPlugin) {
    // No-op
  }

  fun getPlugin(id: String): LensPlugin? = null

  fun getPlugins(): List<LensPlugin> = emptyList()

  // Generic key-value settings store (no-op)
  fun getString(key: String, default: String? = null): String? = default

  fun putString(key: String, value: String?) {
    /* No-op */
  }

  fun getBoolean(key: String, default: Boolean = false): Boolean = default

  fun putBoolean(key: String, value: Boolean) {
    /* No-op */
  }

  /** Returns the original WebViewClient unchanged. */
  fun wrapWebViewClient(client: WebViewClient?): WebViewClient {
    return client ?: object : WebViewClient() {}
  }

  /** Returns the original WebSocketListener unchanged. */
  fun wrapWebSocketListener(listener: WebSocketListener): WebSocketListener {
    return listener
  }

  /** Pass-through interceptor with zero overhead. */
  private object NoOpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      return chain.proceed(chain.request())
    }
  }
}
