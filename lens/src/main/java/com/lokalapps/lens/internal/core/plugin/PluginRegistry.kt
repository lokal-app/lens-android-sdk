package com.lokalapps.lens.internal.core.plugin

import com.lokalapps.lens.api.LensPlugin
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

/**
 * Registry for managing Lens plugins.
 *
 * Handles plugin registration, retrieval, and lifecycle management. Thread-safe for concurrent
 * access.
 */
internal class PluginRegistry {

  private val plugins = ConcurrentHashMap<String, LensPlugin>()

  /**
   * Registers a plugin.
   *
   * If a plugin with the same ID already exists, it will be replaced.
   *
   * @param plugin The plugin to register
   */
  fun register(plugin: LensPlugin) {
    val existing = plugins.put(plugin.id, plugin)
    if (existing != null) {
      Timber.w("Lens: Replaced existing plugin with id '${plugin.id}'")
    } else {
      Timber.d("Lens: Registered plugin '${plugin.name}' (${plugin.id})")
    }
  }

  /**
   * Unregisters a plugin by ID.
   *
   * @param id The plugin ID to unregister
   * @return The removed plugin, or null if not found
   */
  fun unregister(id: String): LensPlugin? {
    val removed = plugins.remove(id)
    if (removed != null) {
      Timber.d("Lens: Unregistered plugin '${removed.name}' ($id)")
    }
    return removed
  }

  /**
   * Gets a plugin by ID.
   *
   * @param id The plugin ID
   * @return The plugin, or null if not found
   */
  fun get(id: String): LensPlugin? = plugins[id]

  /**
   * Gets all registered plugins, sorted by priority (highest first).
   *
   * @return List of plugins sorted by priority
   */
  fun getAll(): List<LensPlugin> {
    return plugins.values.sortedByDescending { it.priority }
  }

  /** Gets the number of registered plugins. */
  val size: Int
    get() = plugins.size

  /**
   * Checks if a plugin is registered.
   *
   * @param id The plugin ID to check
   * @return true if registered, false otherwise
   */
  fun contains(id: String): Boolean = plugins.containsKey(id)

  /** Clears all registered plugins. */
  fun clear() {
    plugins.clear()
    Timber.d("Lens: Cleared all plugins")
  }
}
