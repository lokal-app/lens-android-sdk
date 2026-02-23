@file:OptIn(com.behtar.lens.api.LensExperimental::class)

package com.behtar.lens.internal.interceptors

import com.behtar.lens.api.DefaultHeaderRedactor
import com.behtar.lens.api.HeaderRedactor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderRedactorTest {

  private val redactor: HeaderRedactor = DefaultHeaderRedactor()

  // ── Default redacted headers ─────────────────────────────

  @Test
  fun `redacts Authorization header`() {
    assertTrue(redactor.shouldRedact("Authorization"))
  }

  @Test
  fun `redacts Cookie header`() {
    assertTrue(redactor.shouldRedact("Cookie"))
  }

  @Test
  fun `redacts Set-Cookie header`() {
    assertTrue(redactor.shouldRedact("Set-Cookie"))
  }

  @Test
  fun `redacts X-Api-Key header`() {
    assertTrue(redactor.shouldRedact("X-Api-Key"))
  }

  @Test
  fun `redacts Proxy-Authorization header`() {
    assertTrue(redactor.shouldRedact("Proxy-Authorization"))
  }

  // ── Case insensitivity ───────────────────────────────────

  @Test
  fun `redaction is case insensitive`() {
    assertTrue(redactor.shouldRedact("authorization"))
    assertTrue(redactor.shouldRedact("AUTHORIZATION"))
    assertTrue(redactor.shouldRedact("Authorization"))
    assertTrue(redactor.shouldRedact("cookie"))
    assertTrue(redactor.shouldRedact("COOKIE"))
    assertTrue(redactor.shouldRedact("x-api-key"))
    assertTrue(redactor.shouldRedact("X-API-KEY"))
  }

  // ── Non-sensitive headers ────────────────────────────────

  @Test
  fun `does not redact Content-Type`() {
    assertFalse(redactor.shouldRedact("Content-Type"))
  }

  @Test
  fun `does not redact Accept`() {
    assertFalse(redactor.shouldRedact("Accept"))
  }

  @Test
  fun `does not redact User-Agent`() {
    assertFalse(redactor.shouldRedact("User-Agent"))
  }

  @Test
  fun `does not redact Cache-Control`() {
    assertFalse(redactor.shouldRedact("Cache-Control"))
  }

  // ── Custom redactor ──────────────────────────────────────

  @Test
  fun `custom redactor can redact additional headers`() {
    val custom = HeaderRedactor { name ->
      name.lowercase() in setOf("authorization", "x-custom-secret")
    }

    assertTrue(custom.shouldRedact("Authorization"))
    assertTrue(custom.shouldRedact("X-Custom-Secret"))
    assertFalse(custom.shouldRedact("Content-Type"))
  }
}
