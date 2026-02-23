package com.behtar.lens.internal.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketLogEntryTest {

  // ── host extraction ──────────────────────────────────────

  @Test
  fun `host extracts from wss URL`() {
    val entry = createEntry(url = "wss://example.com/ws/chat")
    assertEquals("example.com", entry.host)
  }

  @Test
  fun `host extracts from ws URL`() {
    val entry = createEntry(url = "ws://localhost:8080/ws")
    assertEquals("localhost", entry.host)
  }

  @Test
  fun `host handles invalid URL gracefully`() {
    val entry = createEntry(url = "not-a-url")
    // Should not throw, returns best-effort
    assertTrue(entry.host.isNotEmpty())
  }

  // ── messageCount ─────────────────────────────────────────

  @Test
  fun `messageCount returns number of messages`() {
    val messages = listOf(createMessage("msg1"), createMessage("msg2"), createMessage("msg3"))
    val entry = createEntry(messages = messages)
    assertEquals(3, entry.messageCount)
  }

  @Test
  fun `messageCount returns 0 for new connection`() {
    val entry = createEntry()
    assertEquals(0, entry.messageCount)
  }

  // ── formattedTimestamp ────────────────────────────────────

  @Test
  fun `formattedTimestamp returns HH mm ss SSS format`() {
    val entry = createEntry()
    assertTrue(entry.formattedTimestamp.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
  }

  // ── Status enum values ───────────────────────────────────

  @Test
  fun `Status enum has all expected values`() {
    val statuses = WebSocketLogEntry.Status.values()
    assertEquals(5, statuses.size)
    assertTrue(statuses.contains(WebSocketLogEntry.Status.CONNECTING))
    assertTrue(statuses.contains(WebSocketLogEntry.Status.OPEN))
    assertTrue(statuses.contains(WebSocketLogEntry.Status.CLOSING))
    assertTrue(statuses.contains(WebSocketLogEntry.Status.CLOSED))
    assertTrue(statuses.contains(WebSocketLogEntry.Status.FAILED))
  }

  // ── Message model ────────────────────────────────────────

  @Test
  fun `Message has correct direction values`() {
    val directions = WebSocketLogEntry.Message.Direction.values()
    assertEquals(2, directions.size)
    assertTrue(directions.contains(WebSocketLogEntry.Message.Direction.SENT))
    assertTrue(directions.contains(WebSocketLogEntry.Message.Direction.RECEIVED))
  }

  @Test
  fun `Message has correct type values`() {
    val types = WebSocketLogEntry.Message.Type.values()
    assertEquals(4, types.size)
    assertTrue(types.contains(WebSocketLogEntry.Message.Type.TEXT))
    assertTrue(types.contains(WebSocketLogEntry.Message.Type.BINARY))
    assertTrue(types.contains(WebSocketLogEntry.Message.Type.PING))
    assertTrue(types.contains(WebSocketLogEntry.Message.Type.PONG))
  }

  @Test
  fun `Message size reflects content length`() {
    val message =
        WebSocketLogEntry.Message(
            direction = WebSocketLogEntry.Message.Direction.RECEIVED,
            type = WebSocketLogEntry.Message.Type.TEXT,
            content = "Hello")
    assertEquals(5L, message.size)
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun createEntry(
      url: String = "wss://example.com/ws",
      status: WebSocketLogEntry.Status = WebSocketLogEntry.Status.CONNECTING,
      messages: List<WebSocketLogEntry.Message> = emptyList()
  ) = WebSocketLogEntry(url = url, status = status, messages = messages)

  private fun createMessage(content: String) =
      WebSocketLogEntry.Message(
          direction = WebSocketLogEntry.Message.Direction.RECEIVED,
          type = WebSocketLogEntry.Message.Type.TEXT,
          content = content)
}
