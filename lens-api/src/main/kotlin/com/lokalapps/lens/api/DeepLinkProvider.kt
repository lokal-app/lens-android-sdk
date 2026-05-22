package com.lokalapps.lens.api

/**
 * Provider interface for Deep Link Tester quick links.
 *
 * Implement this interface to populate the "Quick Links" section in the Lens Deep Link Tester with
 * your app's own deep links. Without a provider, the section is hidden.
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class MyDeepLinkProvider : DeepLinkProvider {
 *     override fun getQuickLinks() = listOf(
 *         DeepLink(label = "Home",    path = "/home"),
 *         DeepLink(label = "Profile", path = "/profile"),
 *         DeepLink(label = "Payment", path = "/payment"),
 *     )
 * }
 * ```
 *
 * ## Registration
 *
 * ```kotlin
 * Lens.install(this) {
 *     deepLinks(MyDeepLinkProvider())
 * }
 * ```
 *
 * ## Path format
 *
 * Paths can be:
 * - **Relative** (`/home`, `/profile?tab=settings`) — Lens prefixes the app's scheme and host
 *   automatically (derived from the package name).
 * - **Absolute** (`myapp://myapp.com/home`, `https://myapp.com/home`) — used as-is.
 */
interface DeepLinkProvider {

  /**
   * Returns the list of quick links to display in the Deep Link Tester.
   *
   * @return List of [DeepLink] entries shown as one-tap shortcuts.
   */
  fun getQuickLinks(): List<DeepLink>
}

/**
 * A single deep link entry for the Deep Link Tester quick links list.
 *
 * @property label Human-readable name shown in the UI (e.g. "Home", "Payment").
 * @property path Relative path or absolute URL (e.g. "/home" or "myapp://myapp.com/home").
 */
data class DeepLink(
    val label: String,
    val path: String,
)
