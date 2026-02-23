package com.behtar.lens.api

/**
 * Provider interface for feature flags.
 *
 * Implement this interface to enable the Feature Flags editor plugin in Lens. This allows
 * developers to toggle feature flags during debugging without rebuilding the app or changing remote
 * config values.
 *
 * **Example Implementation:**
 *
 * ```kotlin
 * class MyFeatureFlagProvider(context: Context) : FeatureFlagProvider {
 *
 *     private val prefs = context.getSharedPreferences("lens_flags", Context.MODE_PRIVATE)
 *
 *     // Define all your feature flags
 *     private val flagDefinitions = listOf(
 *         FeatureFlag(
 *             key = "new_payment_flow",
 *             name = "New Payment Flow",
 *             description = "Use the redesigned payment experience",
 *             type = FlagType.BOOLEAN,
 *             currentValue = prefs.getBoolean("new_payment_flow", false),
 *             defaultValue = false
 *         ),
 *         FeatureFlag(
 *             key = "max_retry_count",
 *             name = "Max Retry Count",
 *             description = "Number of retries for failed API calls",
 *             type = FlagType.INT,
 *             currentValue = prefs.getInt("max_retry_count", 3),
 *             defaultValue = 3
 *         )
 *     )
 *
 *     override fun getFlags(): List<FeatureFlag> {
 *         // Return flags with current values from storage
 *         return flagDefinitions.map { flag ->
 *             flag.copy(currentValue = getCurrentValue(flag))
 *         }
 *     }
 *
 *     override fun setFlag(key: String, value: Any) {
 *         when (value) {
 *             is Boolean -> prefs.edit().putBoolean(key, value).apply()
 *             is String -> prefs.edit().putString(key, value).apply()
 *             is Int -> prefs.edit().putInt(key, value).apply()
 *             is Long -> prefs.edit().putLong(key, value).apply()
 *             is Double -> prefs.edit().putFloat(key, value.toFloat()).apply()
 *         }
 *     }
 *
 *     override fun resetAll() {
 *         prefs.edit().clear().apply()
 *     }
 * }
 * ```
 *
 * **Registration:**
 *
 * ```kotlin
 * Lens.install(app) {
 *     enabled = true
 *     featureFlags(MyFeatureFlagProvider(app))
 * }
 * ```
 */
interface FeatureFlagProvider {

  /**
   * Returns all feature flags with their current values.
   *
   * @return List of [FeatureFlag] objects
   */
  fun getFlags(): List<FeatureFlag>

  /**
   * Updates a feature flag's value.
   *
   * @param key The flag's unique key
   * @param value The new value (must match the flag's type)
   */
  fun setFlag(key: String, value: Any)

  /**
   * Resets all feature flags to their default values. Useful for clearing all overrides at once.
   */
  fun resetAll()
}

/**
 * Represents a feature flag configuration.
 *
 * @property key Unique identifier for this flag (used in code)
 * @property name Human-readable name for display
 * @property description Explanation of what this flag controls
 * @property type The data type of this flag's value
 * @property currentValue The current/overridden value
 * @property defaultValue The default value (used when not overridden)
 */
data class FeatureFlag(
    val key: String,
    val name: String,
    val description: String,
    val type: FlagType,
    val currentValue: Any,
    val defaultValue: Any
)

/** Supported data types for feature flags. */
enum class FlagType {
  /** Boolean flags (on/off, enabled/disabled) */
  BOOLEAN,

  /** String flags (URLs, names, etc.) */
  STRING,

  /** Integer flags (counts, limits, etc.) */
  INT,

  /** Long integer flags (timestamps, large counts) */
  LONG,

  /** Double/float flags (percentages, ratios) */
  DOUBLE
}
