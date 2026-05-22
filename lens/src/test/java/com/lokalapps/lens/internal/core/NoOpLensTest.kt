package com.lokalapps.lens.internal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpLensTest {

  // ── State ────────────────────────────────────────────────

  @Test
  fun `isEnabled returns false`() {
    assertFalse(NoOpLens.isEnabled)
  }

  @Test
  fun `isOpen returns false`() {
    assertFalse(NoOpLens.isOpen)
  }

  // ── Lifecycle methods are no-op ──────────────────────────

  @Test
  fun `initialize does not throw`() {
    NoOpLens.initialize()
  }

  @Test
  fun `open does not throw`() {
    NoOpLens.open()
  }

  @Test
  fun `close does not throw`() {
    NoOpLens.close()
  }

  // ── Plugins ──────────────────────────────────────────────

  @Test
  fun `getPlugin returns null`() {
    assertNull(NoOpLens.getPlugin("network"))
  }

  @Test
  fun `getPlugins returns empty list`() {
    assertTrue(NoOpLens.getPlugins().isEmpty())
  }

  @Test
  fun `registerPlugin is no-op`() {
    // Should not throw
    NoOpLens.registerPlugin(
        object : com.lokalapps.lens.api.ComposableLensPlugin {
          override val id = "test"
          override val name = "Test"
          override val icon = 0
          override val description = "Test"

          @androidx.compose.runtime.Composable override fun Content() {}
        })

    // Still returns null — plugin was not actually registered
    assertNull(NoOpLens.getPlugin("test"))
  }

  // ── Key-Value Store ──────────────────────────────────────

  @Test
  fun `getString returns default`() {
    assertNull(NoOpLens.getString("key", null))
    assertEquals("fallback", NoOpLens.getString("key", "fallback"))
  }

  @Test
  fun `putString is no-op`() {
    NoOpLens.putString("key", "value")
    assertNull(NoOpLens.getString("key", null))
  }

  @Test
  fun `getBoolean returns default`() {
    assertFalse(NoOpLens.getBoolean("key", false))
    assertTrue(NoOpLens.getBoolean("key", true))
  }

  @Test
  fun `putBoolean is no-op`() {
    NoOpLens.putBoolean("key", true)
    assertFalse(NoOpLens.getBoolean("key", false))
  }

  // ── Network Interceptor ──────────────────────────────────

  @Test
  fun `getNetworkInterceptor returns non-null interceptor`() {
    val interceptor = NoOpLens.getNetworkInterceptor()
    assertNotNull(interceptor)
  }
}
