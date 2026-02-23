package com.behtar.lens.api

/**
 * Provider interface for environment configuration.
 *
 * Implement this interface to enable the Environment Switcher plugin in Lens. This allows
 * developers to switch between different server environments (e.g., production, staging,
 * development) during debugging.
 *
 * **Example Implementation:**
 *
 * ```kotlin
 * class MyEnvironmentProvider(context: Context) : EnvironmentProvider {
 *
 *     private val prefs = context.getSharedPreferences("lens_env", Context.MODE_PRIVATE)
 *
 *     private val environments = listOf(
 *         Environment(
 *             id = "prod",
 *             name = "Production",
 *             description = "Live production servers",
 *             baseUrl = "https://api.example.com/"
 *         ),
 *         Environment(
 *             id = "staging",
 *             name = "Staging",
 *             description = "Staging environment for testing",
 *             baseUrl = "https://staging-api.example.com/"
 *         ),
 *         Environment(
 *             id = "dev",
 *             name = "Development",
 *             description = "Local development server",
 *             baseUrl = "http://localhost:8080/"
 *         )
 *     )
 *
 *     override fun getEnvironments() = environments
 *
 *     override fun getCurrentEnvironment(): Environment {
 *         val id = prefs.getString("current_env", "prod")
 *         return environments.find { it.id == id } ?: environments.first()
 *     }
 *
 *     override fun setEnvironment(environment: Environment) {
 *         prefs.edit().putString("current_env", environment.id).apply()
 *         // Note: App restart typically required for this to take effect
 *     }
 * }
 * ```
 *
 * **Registration:**
 *
 * ```kotlin
 * Lens.install(app) {
 *     enabled = true
 *     environments(MyEnvironmentProvider(app))
 * }
 * ```
 */
interface EnvironmentProvider {

  /**
   * Returns the list of available environments. This list is displayed in the Environment Switcher
   * UI.
   *
   * @return List of available [Environment] configurations
   */
  fun getEnvironments(): List<Environment>

  /**
   * Returns the currently selected environment.
   *
   * @return The active [Environment] configuration
   */
  fun getCurrentEnvironment(): Environment

  /**
   * Switches to a different environment.
   *
   * **Important:** Most apps require a restart for environment changes to take effect. Consider
   * showing a dialog to the user asking them to restart the app after calling this method.
   *
   * @param environment The [Environment] to switch to
   */
  fun setEnvironment(environment: Environment)

  /**
   * Called when the user requests to restart the app after changing environment. Default
   * implementation does nothing. Override to provide custom restart behavior.
   */
  fun onRestartRequested() {}

  /**
   * Returns preset WebView environments for the Environment Switcher UI.
   *
   * When non-empty, the Environment Switcher will show a "WebView URL" section with quick-select
   * chips for each preset. Each preset's [Environment.baseUrl] is used as the WebView base URL
   * override.
   *
   * Default implementation returns an empty list (no WebView section shown).
   *
   * @return List of WebView [Environment] presets, or empty list to hide the section
   */
  fun getWebViewPresets(): List<Environment> = emptyList()
}

/**
 * Represents a server environment configuration.
 *
 * @property id Unique identifier for this environment (e.g., "prod", "staging", "dev")
 * @property name Human-readable name (e.g., "Production", "Staging")
 * @property description Brief description of this environment
 * @property baseUrl Base URL for API calls in this environment
 * @property additionalConfig Additional key-value configuration pairs
 */
data class Environment(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val additionalConfig: Map<String, String> = emptyMap()
)
