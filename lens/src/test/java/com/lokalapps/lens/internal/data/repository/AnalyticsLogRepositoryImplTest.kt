package com.lokalapps.lens.internal.data.repository

import app.cash.turbine.test
import com.lokalapps.lens.internal.data.model.AnalyticsLogEntry
import com.lokalapps.lens.internal.data.model.UserPropertyEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyticsLogRepositoryImplTest {

  private lateinit var repository: AnalyticsLogRepositoryImpl

  @Before
  fun setUp() {
    repository = AnalyticsLogRepositoryImpl()
  }

  // ── Events ───────────────────────────────────────────────

  @Test
  fun `logEvent adds event to flow`() = runTest {
    repository.getEvents().test {
      assertEquals(emptyList<AnalyticsLogEntry>(), awaitItem()) // initial

      val event = createEvent("TAP_CARD")
      repository.logEvent(event)

      val events = awaitItem()
      assertEquals(1, events.size)
      assertEquals("TAP_CARD", events[0].eventName)
    }
  }

  @Test
  fun `logEvent adds newest first`() = runTest {
    repository.logEvent(createEvent("FIRST"))
    repository.logEvent(createEvent("SECOND"))

    repository.getEvents().test {
      val events = awaitItem()
      assertEquals("SECOND", events[0].eventName)
      assertEquals("FIRST", events[1].eventName)
    }
  }

  @Test
  fun `logEvent enforces max 1000 events`() {
    repeat(1010) { i -> repository.logEvent(createEvent("EVENT_$i")) }

    runTest {
      repository.getEvents().test {
        val events = awaitItem()
        assertEquals(1000, events.size)
        // Newest should be first
        assertEquals("EVENT_1009", events[0].eventName)
      }
    }
  }

  // ── User Properties ──────────────────────────────────────

  @Test
  fun `logUserProperty adds property to flow`() = runTest {
    repository.getUserProperties().test {
      assertEquals(emptyList<UserPropertyEntry>(), awaitItem()) // initial

      val prop = createUserProperty("user123")
      repository.logUserProperty(prop)

      val props = awaitItem()
      assertEquals(1, props.size)
      assertEquals("user123", props[0].userId)
    }
  }

  @Test
  fun `logUserProperty enforces max 100 entries`() {
    repeat(110) { i -> repository.logUserProperty(createUserProperty("user_$i")) }

    runTest {
      repository.getUserProperties().test {
        val props = awaitItem()
        assertEquals(100, props.size)
      }
    }
  }

  // ── Clear ────────────────────────────────────────────────

  @Test
  fun `clear removes all events and properties`() = runTest {
    repository.logEvent(createEvent("event1"))
    repository.logUserProperty(createUserProperty("user1"))

    repository.clear()

    repository.getEvents().test { assertTrue(awaitItem().isEmpty()) }
    repository.getUserProperties().test { assertTrue(awaitItem().isEmpty()) }
  }

  @Test
  fun `clearEvents only clears events`() = runTest {
    repository.logEvent(createEvent("event1"))
    repository.logUserProperty(createUserProperty("user1"))

    repository.clearEvents()

    repository.getEvents().test { assertTrue(awaitItem().isEmpty()) }
    repository.getUserProperties().test { assertEquals(1, awaitItem().size) }
  }

  @Test
  fun `clearUserProperties only clears properties`() = runTest {
    repository.logEvent(createEvent("event1"))
    repository.logUserProperty(createUserProperty("user1"))

    repository.clearUserProperties()

    repository.getEvents().test { assertEquals(1, awaitItem().size) }
    repository.getUserProperties().test { assertTrue(awaitItem().isEmpty()) }
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun createEvent(
      name: String,
      params: Map<String, Any?> = emptyMap(),
      destinations: Set<String> = setOf("FIREBASE")
  ) = AnalyticsLogEntry(eventName = name, params = params, destinations = destinations)

  private fun createUserProperty(
      userId: String?,
      properties: Map<String, Any?> = mapOf("lang" to "en"),
      destinations: Set<String> = setOf("FIREBASE")
  ) = UserPropertyEntry(userId = userId, properties = properties, destinations = destinations)
}
