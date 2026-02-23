package com.behtar.lens.api

/**
 * Interface for redacting sensitive HTTP headers before they are stored in Lens logs.
 *
 * By default, [DefaultHeaderRedactor] replaces values of well-known sensitive headers
 * (Authorization, Cookie, etc.) with `[REDACTED]`. Implement this interface to customize which
 * headers are redacted.
 *
 * ## Usage:
 * ```kotlin
 * Lens.install(app) {
 *     headerRedactor(HeaderRedactor { name ->
 *         name.equals("X-Custom-Secret", ignoreCase = true)
 *     })
 * }
 * ```
 *
 * @see DefaultHeaderRedactor
 */
@LensExperimental
fun interface HeaderRedactor {

  /**
   * Determines whether the value of a header with the given [name] should be redacted.
   *
   * @param name The header name (e.g., "Authorization", "Cookie")
   * @return `true` if the header value should be replaced with `[REDACTED]`, `false` to log as-is
   */
  fun shouldRedact(name: String): Boolean
}

/**
 * Default header redactor that redacts well-known sensitive headers.
 *
 * Redacted headers:
 * - `Authorization` (Bearer tokens, Basic auth)
 * - `Cookie` / `Set-Cookie` (session tokens)
 * - `X-Api-Key` (API keys)
 * - `Proxy-Authorization` (proxy credentials)
 */
@LensExperimental
class DefaultHeaderRedactor : HeaderRedactor {

  private val sensitiveHeaders =
      setOf("authorization", "cookie", "set-cookie", "x-api-key", "proxy-authorization")

  override fun shouldRedact(name: String): Boolean {
    return name.lowercase() in sensitiveHeaders
  }
}
