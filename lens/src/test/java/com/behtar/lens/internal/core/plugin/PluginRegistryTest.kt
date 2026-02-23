package com.behtar.lens.internal.core.plugin

import com.behtar.lens.api.LensPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PluginRegistryTest {

  private lateinit var registry: PluginRegistry

  @Before
  fun setUp() {
    registry = PluginRegistry()
  }

  // ── Register ─────────────────────────────────────────────

  @Test
  fun `register adds plugin`() {
    registry.register(testPlugin("network", priority = 100))

    assertEquals(1, registry.size)
    assertTrue(registry.contains("network"))
  }

  @Test
  fun `register replaces plugin with same id`() {
    registry.register(testPlugin("network", name = "Network v1", priority = 100))
    registry.register(testPlugin("network", name = "Network v2", priority = 100))

    assertEquals(1, registry.size)
    assertEquals("Network v2", registry.get("network")?.name)
  }

  @Test
  fun `register multiple plugins`() {
    registry.register(testPlugin("network"))
    registry.register(testPlugin("database"))
    registry.register(testPlugin("analytics"))

    assertEquals(3, registry.size)
  }

  // ── Unregister ───────────────────────────────────────────

  @Test
  fun `unregister removes plugin and returns it`() {
    registry.register(testPlugin("network"))

    val removed = registry.unregister("network")
    assertNotNull(removed)
    assertEquals("network", removed!!.id)
    assertEquals(0, registry.size)
  }

  @Test
  fun `unregister returns null for unknown id`() {
    assertNull(registry.unregister("nonexistent"))
  }

  // ── Get ──────────────────────────────────────────────────

  @Test
  fun `get returns plugin by id`() {
    registry.register(testPlugin("network"))
    assertNotNull(registry.get("network"))
  }

  @Test
  fun `get returns null for unknown id`() {
    assertNull(registry.get("nonexistent"))
  }

  // ── GetAll (priority sorting) ────────────────────────────

  @Test
  fun `getAll returns plugins sorted by priority descending`() {
    registry.register(testPlugin("low", priority = 10))
    registry.register(testPlugin("high", priority = 100))
    registry.register(testPlugin("mid", priority = 50))

    val all = registry.getAll()
    assertEquals(3, all.size)
    assertEquals("high", all[0].id)
    assertEquals("mid", all[1].id)
    assertEquals("low", all[2].id)
  }

  @Test
  fun `getAll returns empty list for empty registry`() {
    assertTrue(registry.getAll().isEmpty())
  }

  // ── Contains ─────────────────────────────────────────────

  @Test
  fun `contains returns true for registered plugin`() {
    registry.register(testPlugin("network"))
    assertTrue(registry.contains("network"))
  }

  @Test
  fun `contains returns false for unregistered plugin`() {
    assertFalse(registry.contains("nonexistent"))
  }

  // ── Clear ────────────────────────────────────────────────

  @Test
  fun `clear removes all plugins`() {
    registry.register(testPlugin("a"))
    registry.register(testPlugin("b"))
    registry.register(testPlugin("c"))

    registry.clear()
    assertEquals(0, registry.size)
    assertTrue(registry.getAll().isEmpty())
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun testPlugin(
      id: String,
      name: String = id.replaceFirstChar { it.uppercase() },
      priority: Int = 0
  ): LensPlugin =
      object : LensPlugin {
        override val id = id
        override val name = name
        override val icon = 0
        override val description = "Test plugin $id"
        override val priority = priority
      }
}
