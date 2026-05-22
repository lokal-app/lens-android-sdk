package com.lokalapps.lens.internal.core

import com.lokalapps.lens.api.LensApi
import com.lokalapps.lens.api.LensPlugin
import okhttp3.Interceptor
import okhttp3.Response

/**
 * No-op implementation of Lens when disabled.
 *
 * All methods are empty/no-op, providing zero overhead. This is used when Lens is installed with
 * `enabled = false`.
 */
internal object NoOpLens : LensApi {

  override val isEnabled: Boolean = false
  override val isOpen: Boolean = false

  override fun initialize() {
    // No-op
  }

  override fun open() {
    // No-op
  }

  override fun close() {
    // No-op
  }

  override fun registerPlugin(plugin: LensPlugin) {
    // No-op - plugins are not registered when Lens is disabled
  }

  override fun getPlugin(id: String): LensPlugin? = null

  override fun getPlugins(): List<LensPlugin> = emptyList()

  // Generic key-value settings store (no-op)
  override fun getString(key: String, default: String?): String? = default

  override fun putString(key: String, value: String?) {
    /* No-op */
  }

  override fun getBoolean(key: String, default: Boolean): Boolean = default

  override fun putBoolean(key: String, value: Boolean) {
    /* No-op */
  }

  /** Returns a no-op interceptor that simply passes requests through. */
  override fun getNetworkInterceptor(): Interceptor = NoOpInterceptor

  /** Interceptor that does nothing - just passes requests through. */
  private object NoOpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      return chain.proceed(chain.request())
    }
  }
}
