package com.behtar.lens.sample

import android.content.Context
import android.content.Intent
import com.behtar.lens.api.Environment
import com.behtar.lens.api.EnvironmentProvider

/**
 * Sample EnvironmentProvider demonstrating the correct implementation pattern.
 *
 * Key points:
 * - setEnvironment() uses commit() (synchronous) not apply() (async) — the process is killed
 *   immediately after, and apply() may not flush before the process dies.
 * - onRestartRequested() fires a launch intent before killProcess() to guarantee a true cold start,
 *   so the DI graph (and any singleton base URLs) is rebuilt from scratch.
 * - getCurrentEnvironment() and your network layer must both read the same SharedPrefs key —
 *   BuildConfig is a compile-time constant and cannot reflect a runtime selection.
 */
class SampleEnvironmentProvider(private val context: Context) : EnvironmentProvider {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val environments =
      listOf(
          Environment(
              id = "prod",
              name = "Production",
              description = "Live servers",
              baseUrl = "https://api.example.com/"),
          Environment(
              id = "staging",
              name = "Staging",
              description = "Staging servers",
              baseUrl = "https://staging.example.com/"),
          Environment(
              id = "dev",
              name = "Development",
              description = "Local server (Android emulator)",
              baseUrl = "http://10.0.2.2:8080/"))

  override fun getEnvironments() = environments

  override fun getCurrentEnvironment(): Environment {
    val id = prefs.getString(KEY_ENV_ID, null) ?: DEFAULT_ENV_ID
    return environments.find { it.id == id } ?: environments.first()
  }

  override fun setEnvironment(environment: Environment) {
    // commit() not apply() — process is killed right after this returns.
    prefs.edit().putString(KEY_ENV_ID, environment.id).commit()
  }

  override fun onRestartRequested() {
    // Schedule a clean launch intent before killing the process. A bare killProcess() can
    // restore the previous back stack without going through Application.onCreate(), meaning
    // singletons (e.g. Retrofit with a fixed base URL) won't be rebuilt with the new value.
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent?.let { context.startActivity(it) }
    android.os.Process.killProcess(android.os.Process.myPid())
  }

  companion object {
    const val PREFS_NAME = "lens_env"
    const val KEY_ENV_ID = "env_id"
    const val DEFAULT_ENV_ID = "prod"

    /**
     * Call this from your network layer's base URL provider at startup. Both this and
     * [SampleEnvironmentProvider] must read the same prefs file and key.
     */
    fun resolveBaseUrl(context: Context): String {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return when (prefs.getString(KEY_ENV_ID, null) ?: DEFAULT_ENV_ID) {
        "staging" -> "https://staging.example.com/"
        "dev" -> "http://10.0.2.2:8080/"
        else -> "https://api.example.com/"
      }
    }
  }
}
