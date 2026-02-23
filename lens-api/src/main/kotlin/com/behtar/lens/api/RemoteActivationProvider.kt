package com.behtar.lens.api

/**
 * Provider for remote activation/deactivation of Lens.
 *
 * Implement this interface to control Lens availability via a remote service (e.g., Firebase Remote
 * Config, LaunchDarkly, custom backend).
 *
 * When configured, Lens will only activate if both [LensConfig.enabled] is `true` AND this provider
 * returns `true`.
 *
 * ## Example (Firebase Remote Config):
 * ```kotlin
 * class FirebaseRemoteActivationProvider(
 *     private val key: String
 * ) : RemoteActivationProvider {
 *     override fun isEnabled(callback: (Boolean) -> Unit) {
 *         val remoteConfig = FirebaseRemoteConfig.getInstance()
 *         callback(remoteConfig.getBoolean(key))
 *     }
 * }
 * ```
 *
 * ## Example (always enabled):
 * ```kotlin
 * RemoteActivationProvider { callback -> callback(true) }
 * ```
 *
 * @see LensConfig.Builder for how to configure this provider
 */
fun interface RemoteActivationProvider {

  /**
   * Checks whether Lens should be enabled remotely.
   *
   * This is called during [Lens.install] to determine if Lens should activate. The callback pattern
   * allows for async remote config fetches.
   *
   * @param callback Invoke with `true` to enable Lens, `false` to disable
   */
  fun isEnabled(callback: (Boolean) -> Unit)
}
