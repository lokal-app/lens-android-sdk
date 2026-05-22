package com.lokalapps.lens.internal.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsLogEntryTest {

  // ── displayDestination ───────────────────────────────────

  @Test
  fun `displayDestination returns name for single destination`() {
    val entry = createEntry(destinations = setOf("FIREBASE"))
    assertEquals("FIREBASE", entry.displayDestination)
  }

  @Test
  fun `displayDestination returns null for multiple destinations`() {
    val entry = createEntry(destinations = setOf("FIREBASE", "MOENGAGE"))
    assertNull(entry.displayDestination)
  }

  // ── paramCount ───────────────────────────────────────────

  @Test
  fun `paramCount returns number of params`() {
    val entry = createEntry(params = mapOf("key1" to "val1", "key2" to "val2"))
    assertEquals(2, entry.paramCount)
  }

  @Test
  fun `paramCount returns 0 for empty params`() {
    val entry = createEntry(params = emptyMap())
    assertEquals(0, entry.paramCount)
  }

  // ── summary ──────────────────────────────────────────────

  @Test
  fun `summary includes event name`() {
    val entry = createEntry(eventName = "TAP_CARD")
    assertTrue(entry.summary.startsWith("TAP_CARD"))
  }

  @Test
  fun `summary includes param count when non-zero`() {
    val entry = createEntry(eventName = "TAP_CARD", params = mapOf("card_id" to "123"))
    assertEquals("TAP_CARD (1 params)", entry.summary)
  }

  @Test
  fun `summary includes revenue for revenue events`() {
    val entry = createEntry(eventName = "PURCHASE", isRevenueEvent = true, revenueAmount = 299.0)
    assertTrue(entry.summary.contains("₹299.0"))
  }

  @Test
  fun `summary omits param count when zero`() {
    val entry = createEntry(eventName = "SIMPLE_EVENT", params = emptyMap())
    assertEquals("SIMPLE_EVENT", entry.summary)
  }

  // ── formattedTime ────────────────────────────────────────

  @Test
  fun `formattedTime returns HH mm ss SSS format`() {
    val entry = createEntry()
    // Just verify it's not empty and has expected format (XX:XX:XX.XXX)
    assertTrue(entry.formattedTime.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun createEntry(
      eventName: String = "TEST_EVENT",
      params: Map<String, Any?> = emptyMap(),
      destinations: Set<String> = setOf("FIREBASE"),
      isRevenueEvent: Boolean = false,
      revenueAmount: Double? = null
  ) =
      AnalyticsLogEntry(
          eventName = eventName,
          params = params,
          destinations = destinations,
          isRevenueEvent = isRevenueEvent,
          revenueAmount = revenueAmount)
}
