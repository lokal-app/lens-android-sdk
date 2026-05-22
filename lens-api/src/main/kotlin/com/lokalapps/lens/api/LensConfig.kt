@file:OptIn(LensExperimental::class)

package com.lokalapps.lens.api

/**
 * Configuration for Lens SDK.
 *
 * Use the [Builder] class via DSL to create a configuration:
 * ```kotlin
 * Lens.install(app) {
 *     enabled = true
 *     activationGesture = ActivationGesture.FIVE_TAP
 *     shakeToOpenEnabled = true
 *
 *     // Optional: remote kill switch
 *     remoteActivation(MyRemoteActivationProvider())
 *
 *     environments(MyEnvironmentProvider())
 *     featureFlags(MyFeatureFlagProvider())
 *     quickActions(MyQuickActionsProvider())
 * }
 * ```
 */
data class LensConfig(
    /** Whether Lens is enabled (master switch) */
    val enabled: Boolean,

    /** Provider for remote enable/disable (e.g., Firebase Remote Config, LaunchDarkly) */
    val remoteActivationProvider: RemoteActivationProvider?,

    /** Gesture type to activate Lens dashboard */
    val activationGesture: ActivationGesture,

    /** Whether shaking the device opens Lens */
    val shakeToOpenEnabled: Boolean,

    /** Shake sensitivity (higher = less sensitive). Default is 2.5 */
    val shakeThreshold: Float,

    /** Provider for environment switching */
    val environmentProvider: EnvironmentProvider?,

    /** Provider for feature flags */
    val featureFlagProvider: FeatureFlagProvider?,

    /** Provider for quick actions */
    val quickActionsProvider: QuickActionsProvider?,

    /** Provider for deep link tester quick links */
    val deepLinkProvider: DeepLinkProvider?,

    /** Header redactor for network log security */
    val headerRedactor: HeaderRedactor,

    /** Whether to show a sticky notification with request/error counts */
    val showNotification: Boolean
) {

  /** Builder for [LensConfig] using Kotlin DSL. */
  class Builder {
    /**
     * Master switch for Lens. Set to `true` to enable Lens, `false` to disable entirely.
     *
     * Recommended:
     * ```kotlin
     * enabled = BuildConfig.DEBUG || BuildConfig.INTERNAL_TOOLS_ENABLED
     * ```
     */
    var enabled: Boolean = false

    private var remoteActivationProvider: RemoteActivationProvider? = null

    /** Gesture type to activate Lens dashboard. Default: [ActivationGesture.FIVE_TAP] */
    var activationGesture: ActivationGesture = ActivationGesture.FIVE_TAP

    /** Whether shaking the device opens Lens. Default: true */
    var shakeToOpenEnabled: Boolean = true

    /**
     * Shake detection sensitivity threshold. Higher values = less sensitive (requires harder
     * shake). Lower values = more sensitive (easier to trigger). Default: 2.5
     */
    var shakeThreshold: Float = 2.5f

    private var environmentProvider: EnvironmentProvider? = null
    private var featureFlagProvider: FeatureFlagProvider? = null
    private var quickActionsProvider: QuickActionsProvider? = null
    private var deepLinkProvider: DeepLinkProvider? = null
    private var headerRedactor: HeaderRedactor = DefaultHeaderRedactor()

    /**
     * Whether to show a sticky notification with live request/error counts.
     *
     * When enabled, Lens displays a persistent notification while active:
     * - Shows total request count and error count in real time
     * - Tapping opens the Lens dashboard
     * - "Clear" action resets all network and exception logs
     *
     * On Android 13+ (API 33), requires `POST_NOTIFICATIONS` permission. If the permission is not
     * granted, the notification is silently skipped.
     *
     * Default: true
     */
    var showNotification: Boolean = true

    /**
     * Sets a remote activation provider for remote enable/disable.
     *
     * When set, Lens will only activate if both [enabled] is true AND this provider returns true.
     * This allows you to remotely disable Lens without an app update.
     *
     * @param provider Implementation of [RemoteActivationProvider]
     * @sample
     *
     * ```kotlin
     * // Firebase Remote Config example:
     * remoteActivation(RemoteActivationProvider { callback ->
     *     callback(FirebaseRemoteConfig.getInstance().getBoolean("lens_enabled"))
     * })
     * ```
     */
    fun remoteActivation(provider: RemoteActivationProvider) {
      remoteActivationProvider = provider
    }

    /**
     * Sets the environment provider for the Environment Switcher plugin.
     *
     * @param provider Implementation of [EnvironmentProvider]
     */
    fun environments(provider: EnvironmentProvider) {
      environmentProvider = provider
    }

    /**
     * Sets the feature flag provider for the Feature Flags plugin.
     *
     * @param provider Implementation of [FeatureFlagProvider]
     */
    fun featureFlags(provider: FeatureFlagProvider) {
      featureFlagProvider = provider
    }

    /**
     * Sets the quick actions provider for the Quick Actions plugin.
     *
     * @param provider Implementation of [QuickActionsProvider]
     */
    fun quickActions(provider: QuickActionsProvider) {
      quickActionsProvider = provider
    }

    /**
     * Sets the deep link provider for the Deep Link Tester quick links.
     *
     * When set, the Deep Link Tester shows a "Quick Links" section with one-tap shortcuts to your
     * app's deep links. Without a provider, the section is hidden.
     *
     * @param provider Implementation of [DeepLinkProvider]
     */
    fun deepLinks(provider: DeepLinkProvider) {
      deepLinkProvider = provider
    }

    /**
     * Sets a custom header redactor for network log security.
     *
     * The redactor determines which HTTP header values are replaced with `[REDACTED]` before being
     * stored in Lens network logs. By default, [DefaultHeaderRedactor] redacts Authorization,
     * Cookie, Set-Cookie, X-Api-Key, and Proxy-Authorization.
     *
     * @param redactor Implementation of [HeaderRedactor]
     */
    fun headerRedactor(redactor: HeaderRedactor) {
      headerRedactor = redactor
    }

    /** Builds the [LensConfig] with the current settings. */
    fun build() =
        LensConfig(
            enabled = enabled,
            remoteActivationProvider = remoteActivationProvider,
            activationGesture = activationGesture,
            shakeToOpenEnabled = shakeToOpenEnabled,
            shakeThreshold = shakeThreshold,
            environmentProvider = environmentProvider,
            featureFlagProvider = featureFlagProvider,
            quickActionsProvider = quickActionsProvider,
            deepLinkProvider = deepLinkProvider,
            headerRedactor = headerRedactor,
            showNotification = showNotification)
  }
}

/** Gesture types for activating Lens dashboard. */
enum class ActivationGesture {
  /**
   * Tap the screen 3 times rapidly to open Lens. Faster to trigger, but more likely to be
   * accidentally triggered.
   */
  THREE_TAP,

  /**
   * Tap the screen 5 times rapidly to open Lens. Balanced between accessibility and accidental
   * triggers. This is the default.
   */
  FIVE_TAP,

  /**
   * Long press anywhere on the screen to open Lens. May conflict with other long press handlers.
   */
  LONG_PRESS,

  /**
   * Disable gesture activation entirely. Lens can only be opened programmatically via
   * [Lens.open()].
   */
  NONE
}
