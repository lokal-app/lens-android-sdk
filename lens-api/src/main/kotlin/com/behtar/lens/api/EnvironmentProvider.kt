package com.behtar.lens.api

/**
 * Provider interface for environment configuration.
 *
 * Implement this interface to enable the Environment Switcher plugin in Lens. Lens shows a built-in
 * UI with a confirmation dialog and restart button — you do not need to build any UI. Your job is
 * to (1) persist the selection and (2) trigger a process restart.
 *
 * ## How it works
 *
 * When the user picks an environment in Lens and confirms the dialog, Lens calls:
 * 1. [setEnvironment] — persist the selection
 * 2. [onRestartRequested] — restart the process so the new base URL takes effect
 *
 * On next cold start, [getCurrentEnvironment] is called by Lens to show which environment is
 * active. Your networking layer reads the same persisted value to pick the right base URL.
 *
 * ## Critical implementation notes
 *
 * **Use `commit()` not `apply()` in [setEnvironment].** `apply()` is asynchronous — the process is
 * killed immediately after, and the write may not flush to disk before the process dies, silently
 * losing the selection. `commit()` is synchronous and guaranteed to persist first.
 *
 * **Fire a launch intent before killing the process in [onRestartRequested].** A bare
 * `Process.killProcess()` can cause Android to restore the previous back stack without going
 * through a clean cold start, meaning your DI graph (Hilt/Dagger singletons) may be rebuilt but
 * from a task-restore path that skips your Application-level init. Schedule the launch intent first
 * to guarantee a true cold start.
 *
 * **Your network layer must read from the same SharedPrefs at startup**, not just from
 * `BuildConfig`. `BuildConfig` fields are compile-time constants — switching environments at
 * runtime means your base URL source needs a mutable layer (SharedPrefs) that takes precedence over
 * the compile-time default.
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class MyEnvironmentProvider(private val context: Context) : EnvironmentProvider {
 *
 *     private val prefs = context.getSharedPreferences("lens_env", Context.MODE_PRIVATE)
 *
 *     private val environments = listOf(
 *         Environment(id = "prod", name = "Production",
 *             description = "Live servers", baseUrl = "https://api.example.com/"),
 *         Environment(id = "staging", name = "Staging",
 *             description = "Staging servers", baseUrl = "https://staging.example.com/"),
 *         Environment(id = "dev", name = "Development",
 *             description = "Local server", baseUrl = "http://10.0.2.2:8080/")
 *     )
 *
 *     // Default environment based on build variant — override with persisted selection if present.
 *     private val defaultId = if (BuildConfig.DEBUG) "dev" else "prod"
 *
 *     override fun getEnvironments() = environments
 *
 *     override fun getCurrentEnvironment(): Environment {
 *         val id = prefs.getString("env_id", null) ?: defaultId
 *         return environments.find { it.id == id } ?: environments.first()
 *     }
 *
 *     override fun setEnvironment(environment: Environment) {
 *         // commit() not apply() — process is killed immediately after this call.
 *         // apply() is async and may not flush before the process dies.
 *         prefs.edit().putString("env_id", environment.id).commit()
 *     }
 *
 *     override fun onRestartRequested() {
 *         // Schedule a clean launch intent before killing — ensures a true cold start
 *         // so your DI graph (Hilt/Dagger singletons) is rebuilt from scratch.
 *         val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
 *         intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
 *         intent?.let { context.startActivity(it) }
 *         Process.killProcess(Process.myPid())
 *     }
 * }
 * ```
 *
 * ## Wiring your network layer
 *
 * The environment override only takes effect if your network layer reads the persisted value at
 * startup. For example, if you use Hilt/Dagger with a `@Singleton` Retrofit, your base URL provider
 * must check SharedPrefs before falling back to `BuildConfig`:
 * ```kotlin
 * fun resolveBaseUrl(context: Context): String {
 *     val prefs = context.getSharedPreferences("lens_env", Context.MODE_PRIVATE)
 *     return when (prefs.getString("env_id", null)) {
 *         "staging" -> "https://staging.example.com/"
 *         "dev"     -> "http://10.0.2.2:8080/"
 *         else      -> "https://api.example.com/"  // prod or no override
 *     }
 * }
 * ```
 *
 * **Registration:**
 *
 * ```kotlin
 * Lens.install(app) {
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
   * Persists the selected environment. Called by Lens before [onRestartRequested].
   *
   * **Use `commit()` not `apply()`** — the process is killed immediately after this returns.
   * `apply()` writes asynchronously and may not flush to disk before the process dies.
   *
   * @param environment The [Environment] to switch to
   */
  fun setEnvironment(environment: Environment)

  /**
   * Restarts the app so the new environment takes effect. Called by Lens after [setEnvironment].
   *
   * **Override this** — the default implementation does nothing, so the environment selection is
   * saved but never applied. Schedule a launch intent before killing the process to guarantee a
   * true cold start (see class-level docs for the recommended implementation).
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
