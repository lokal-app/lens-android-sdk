package com.lokalapps.lens.internal.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogEntryTest {

  // ── durationString ───────────────────────────────────────

  @Test
  fun `durationString formats milliseconds`() {
    val entry = createEntry(durationMs = 150)
    assertEquals("150ms", entry.durationString)
  }

  @Test
  fun `durationString formats seconds`() {
    val entry = createEntry(durationMs = 1500)
    assertEquals("1.50s", entry.durationString)
  }

  @Test
  fun `durationString formats zero`() {
    val entry = createEntry(durationMs = 0)
    assertEquals("0ms", entry.durationString)
  }

  @Test
  fun `durationString formats sub-second boundary`() {
    val entry = createEntry(durationMs = 999)
    assertEquals("999ms", entry.durationString)
  }

  @Test
  fun `durationString formats exactly one second`() {
    val entry = createEntry(durationMs = 1000)
    assertEquals("1.00s", entry.durationString)
  }

  // ── responseSizeString ───────────────────────────────────

  @Test
  fun `responseSizeString formats bytes`() {
    val entry = createEntry(responseBodySize = 512)
    assertEquals("512 B", entry.responseSizeString)
  }

  @Test
  fun `responseSizeString formats kilobytes`() {
    val entry = createEntry(responseBodySize = 2048)
    assertEquals("2.0 KB", entry.responseSizeString)
  }

  @Test
  fun `responseSizeString formats megabytes`() {
    val entry = createEntry(responseBodySize = 2 * 1024 * 1024)
    assertEquals("2.00 MB", entry.responseSizeString)
  }

  // ── statusType ───────────────────────────────────────────

  @Test
  fun `statusType returns PENDING when in progress`() {
    val entry = createEntry(isInProgress = true)
    assertEquals(NetworkLogEntry.StatusType.PENDING, entry.statusType)
  }

  @Test
  fun `statusType returns ERROR when has error message`() {
    val entry = createEntry(isInProgress = false, errorMessage = "Connection refused")
    assertEquals(NetworkLogEntry.StatusType.ERROR, entry.statusType)
  }

  @Test
  fun `statusType returns SUCCESS for 2xx`() {
    assertEquals(
        NetworkLogEntry.StatusType.SUCCESS,
        createEntry(isInProgress = false, responseCode = 200).statusType)
    assertEquals(
        NetworkLogEntry.StatusType.SUCCESS,
        createEntry(isInProgress = false, responseCode = 201).statusType)
    assertEquals(
        NetworkLogEntry.StatusType.SUCCESS,
        createEntry(isInProgress = false, responseCode = 299).statusType)
  }

  @Test
  fun `statusType returns REDIRECT for 3xx`() {
    assertEquals(
        NetworkLogEntry.StatusType.REDIRECT,
        createEntry(isInProgress = false, responseCode = 301).statusType)
    assertEquals(
        NetworkLogEntry.StatusType.REDIRECT,
        createEntry(isInProgress = false, responseCode = 302).statusType)
  }

  @Test
  fun `statusType returns CLIENT_ERROR for 4xx`() {
    assertEquals(
        NetworkLogEntry.StatusType.CLIENT_ERROR,
        createEntry(isInProgress = false, responseCode = 400).statusType)
    assertEquals(
        NetworkLogEntry.StatusType.CLIENT_ERROR,
        createEntry(isInProgress = false, responseCode = 404).statusType)
  }

  @Test
  fun `statusType returns SERVER_ERROR for 5xx`() {
    assertEquals(
        NetworkLogEntry.StatusType.SERVER_ERROR,
        createEntry(isInProgress = false, responseCode = 500).statusType)
    assertEquals(
        NetworkLogEntry.StatusType.SERVER_ERROR,
        createEntry(isInProgress = false, responseCode = 503).statusType)
  }

  @Test
  fun `statusType ERROR takes precedence over status code`() {
    // Even with a 200 response code, an error message makes it ERROR
    val entry = createEntry(isInProgress = false, responseCode = 200, errorMessage = "Timeout")
    assertEquals(NetworkLogEntry.StatusType.ERROR, entry.statusType)
  }

  @Test
  fun `statusType PENDING takes precedence over everything`() {
    val entry = createEntry(isInProgress = true, responseCode = 200, errorMessage = "error")
    assertEquals(NetworkLogEntry.StatusType.PENDING, entry.statusType)
  }

  // ── toCurlCommand ────────────────────────────────────────

  @Test
  fun `toCurlCommand generates basic GET`() {
    val entry = createEntry(method = "GET", url = "https://api.example.com/users")
    val curl = entry.toCurlCommand()

    assertTrue(curl.startsWith("curl"))
    assertTrue(curl.contains("'https://api.example.com/users'"))
    // GET is default, so -X GET should NOT be present
    assertTrue(!curl.contains("-X GET"))
  }

  @Test
  fun `toCurlCommand includes method for non-GET`() {
    val entry = createEntry(method = "POST", url = "https://api.example.com/users")
    val curl = entry.toCurlCommand()

    assertTrue(curl.contains("-X POST"))
  }

  @Test
  fun `toCurlCommand includes headers`() {
    val entry =
        createEntry(
            requestHeaders =
                mapOf("Content-Type" to "application/json", "Authorization" to "Bearer token123"))
    val curl = entry.toCurlCommand()

    assertTrue(curl.contains("-H 'Content-Type: application/json'"))
    assertTrue(curl.contains("-H 'Authorization: Bearer token123'"))
  }

  @Test
  fun `toCurlCommand includes compressed flag for gzip`() {
    val entry = createEntry(requestHeaders = mapOf("Accept-Encoding" to "gzip, deflate"))
    val curl = entry.toCurlCommand()

    assertTrue(curl.contains("--compressed"))
  }

  @Test
  fun `toCurlCommand includes body for POST`() {
    val entry = createEntry(method = "POST", requestBody = """{"name": "John"}""")
    val curl = entry.toCurlCommand()

    assertTrue(curl.contains("-d '{\"name\": \"John\"}'"))
  }

  @Test
  fun `toCurlCommand does not include body for GET`() {
    val entry = createEntry(method = "GET", requestBody = "should not appear")
    val curl = entry.toCurlCommand()

    assertTrue(!curl.contains("-d"))
  }

  @Test
  fun `toCurlCommandOneLine removes line breaks`() {
    val entry =
        createEntry(
            method = "POST",
            requestHeaders = mapOf("Content-Type" to "application/json"),
            requestBody = """{"key": "value"}""")
    val oneLine = entry.toCurlCommandOneLine()

    assertTrue(!oneLine.contains("\n"))
    assertTrue(!oneLine.contains("\\"))
  }

  @Test
  fun `toCurlCommand escapes single quotes in headers`() {
    val entry = createEntry(requestHeaders = mapOf("Custom" to "value'with'quotes"))
    val curl = entry.toCurlCommand()

    assertTrue(curl.contains("value'\\''with'\\''quotes"))
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun createEntry(
      method: String = "GET",
      url: String = "https://api.example.com/test",
      responseCode: Int = 0,
      isInProgress: Boolean = true,
      isSuccessful: Boolean = false,
      errorMessage: String? = null,
      durationMs: Long = 0,
      responseBodySize: Long = 0,
      requestHeaders: Map<String, String> = emptyMap(),
      requestBody: String? = null
  ) =
      NetworkLogEntry(
          method = method,
          url = url,
          host = "api.example.com",
          path = "/test",
          responseCode = responseCode,
          isInProgress = isInProgress,
          isSuccessful = isSuccessful,
          errorMessage = errorMessage,
          durationMs = durationMs,
          responseBodySize = responseBodySize,
          requestHeaders = requestHeaders,
          requestBody = requestBody)
}
