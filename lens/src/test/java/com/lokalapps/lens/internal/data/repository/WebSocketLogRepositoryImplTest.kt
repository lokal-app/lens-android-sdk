package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.WebSocketLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebSocketLogRepositoryImplTest {

  private lateinit var repository: WebSocketLogRepositoryImpl

  @Before
  fun setUp() {
    repository = WebSocketLogRepositoryImpl()
  }

  // ── Connection lifecycle ─────────────────────────────────

  @Test
  fun `onConnecting creates a new connection entry`() {
    val id = repository.onConnecting("wss://example.com/ws")

    assertEquals(1, repository.connections.value.size)
    val connection = repository.connections.value[0]
    assertEquals(id, connection.id)
    assertEquals("wss://example.com/ws", connection.url)
    assertEquals(WebSocketLogEntry.Status.CONNECTING, connection.status)
  }

  @Test
  fun `onOpen updates status to OPEN`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onOpen(id)

    val connection = repository.connections.value[0]
    assertEquals(WebSocketLogEntry.Status.OPEN, connection.status)
  }

  @Test
  fun `onClosing updates status with close code and reason`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onOpen(id)
    repository.onClosing(id, 1000, "Normal closure")

    val connection = repository.connections.value[0]
    assertEquals(WebSocketLogEntry.Status.CLOSING, connection.status)
    assertEquals(1000, connection.closeCode)
    assertEquals("Normal closure", connection.closeReason)
  }

  @Test
  fun `onClosed updates status to CLOSED`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onOpen(id)
    repository.onClosed(id, 1000, "Normal closure")

    val connection = repository.connections.value[0]
    assertEquals(WebSocketLogEntry.Status.CLOSED, connection.status)
  }

  @Test
  fun `onFailure updates status to FAILED with error`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onFailure(id, "Connection refused")

    val connection = repository.connections.value[0]
    assertEquals(WebSocketLogEntry.Status.FAILED, connection.status)
    assertEquals("Connection refused", connection.errorMessage)
  }

  // ── Messages ─────────────────────────────────────────────

  @Test
  fun `onMessage adds message to connection`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onOpen(id)
    repository.onMessage(
        id,
        WebSocketLogEntry.Message.Direction.RECEIVED,
        WebSocketLogEntry.Message.Type.TEXT,
        "Hello, World!")

    val connection = repository.connections.value[0]
    assertEquals(1, connection.messages.size)
    assertEquals("Hello, World!", connection.messages[0].content)
    assertEquals(WebSocketLogEntry.Message.Direction.RECEIVED, connection.messages[0].direction)
  }

  @Test
  fun `onMessage trims to max 100 messages per connection`() {
    val id = repository.onConnecting("wss://example.com/ws")
    repository.onOpen(id)

    repeat(110) { i ->
      repository.onMessage(
          id,
          WebSocketLogEntry.Message.Direction.RECEIVED,
          WebSocketLogEntry.Message.Type.TEXT,
          "Message $i")
    }

    val connection = repository.connections.value[0]
    assertEquals(100, connection.messages.size)
    // takeLast keeps the last 100 (messages 10-109)
    assertEquals("Message 10", connection.messages[0].content)
  }

  @Test
  fun `onMessage truncates content to 10000 chars`() {
    val id = repository.onConnecting("wss://example.com/ws")
    val longContent = "x".repeat(15000)

    repository.onMessage(
        id,
        WebSocketLogEntry.Message.Direction.RECEIVED,
        WebSocketLogEntry.Message.Type.TEXT,
        longContent)

    val message = repository.connections.value[0].messages[0]
    assertEquals(10000, message.content.length)
  }

  // ── Active count ─────────────────────────────────────────

  @Test
  fun `activeCount reflects open connections`() {
    val id1 = repository.onConnecting("wss://example.com/ws1")
    val id2 = repository.onConnecting("wss://example.com/ws2")

    assertEquals(0, repository.activeCount) // both CONNECTING

    repository.onOpen(id1)
    assertEquals(1, repository.activeCount)

    repository.onOpen(id2)
    assertEquals(2, repository.activeCount)

    repository.onClosed(id1, 1000, "done")
    assertEquals(1, repository.activeCount)
  }

  // ── Ordering ─────────────────────────────────────────────

  @Test
  fun `connections are newest first`() {
    repository.onConnecting("wss://example.com/first")
    repository.onConnecting("wss://example.com/second")

    assertEquals("wss://example.com/second", repository.connections.value[0].url)
    assertEquals("wss://example.com/first", repository.connections.value[1].url)
  }

  // ── Max connections ──────────────────────────────────────

  @Test
  fun `max 50 connections enforced`() {
    repeat(55) { i -> repository.onConnecting("wss://example.com/ws$i") }

    assertEquals(50, repository.connections.value.size)
  }

  // ── Clear ────────────────────────────────────────────────

  @Test
  fun `clear removes all connections`() {
    repeat(5) { repository.onConnecting("wss://example.com/ws") }
    assertEquals(5, repository.connections.value.size)

    repository.clear()
    assertTrue(repository.connections.value.isEmpty())
  }
}
