package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.NetworkLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkLogRepositoryImplTest {

  private lateinit var repository: NetworkLogRepositoryImpl

  @Before
  fun setUp() {
    repository = NetworkLogRepositoryImpl()
  }

  // ── Add/Get ──────────────────────────────────────────────

  @Test
  fun `addEntry adds entry to logs`() {
    val entry = createEntry(url = "https://example.com/api/test")
    repository.addEntry(entry)

    assertEquals(1, repository.entryCount)
    assertEquals(entry, repository.logs.value.first())
  }

  @Test
  fun `addEntry adds newest first`() {
    val entry1 = createEntry(url = "https://example.com/first")
    val entry2 = createEntry(url = "https://example.com/second")

    repository.addEntry(entry1)
    repository.addEntry(entry2)

    assertEquals("https://example.com/second", repository.logs.value[0].url)
    assertEquals("https://example.com/first", repository.logs.value[1].url)
  }

  @Test
  fun `getEntry returns correct entry by id`() {
    val entry = createEntry(id = "test-id-123")
    repository.addEntry(entry)

    val found = repository.getEntry("test-id-123")
    assertNotNull(found)
    assertEquals("test-id-123", found!!.id)
  }

  @Test
  fun `getEntry returns null for unknown id`() {
    assertNull(repository.getEntry("nonexistent"))
  }

  // ── Update ───────────────────────────────────────────────

  @Test
  fun `updateEntry modifies existing entry`() {
    val entry = createEntry(id = "update-me", responseCode = 0, isInProgress = true)
    repository.addEntry(entry)

    repository.updateEntry("update-me") {
      it.copy(responseCode = 200, isInProgress = false, isSuccessful = true)
    }

    val updated = repository.getEntry("update-me")!!
    assertEquals(200, updated.responseCode)
    assertEquals(false, updated.isInProgress)
    assertEquals(true, updated.isSuccessful)
  }

  @Test
  fun `updateEntry does nothing for unknown id`() {
    val entry = createEntry(id = "existing")
    repository.addEntry(entry)

    repository.updateEntry("unknown") { it.copy(responseCode = 500) }

    // Original entry unchanged
    assertEquals(0, repository.getEntry("existing")!!.responseCode)
  }

  // ── Clear ────────────────────────────────────────────────

  @Test
  fun `clear removes all entries`() {
    repeat(10) { repository.addEntry(createEntry()) }
    assertEquals(10, repository.entryCount)

    repository.clear()
    assertEquals(0, repository.entryCount)
    assertTrue(repository.logs.value.isEmpty())
  }

  // ── Max entries limit ────────────────────────────────────

  @Test
  fun `addEntry enforces max 500 entries`() {
    repeat(510) { i -> repository.addEntry(createEntry(url = "https://example.com/$i")) }

    assertEquals(500, repository.entryCount)
    // Newest entry (509) should be first
    assertEquals("https://example.com/509", repository.logs.value.first().url)
  }

  // ── Search ───────────────────────────────────────────────

  @Test
  fun `searchLogs filters by url`() {
    repository.addEntry(createEntry(url = "https://api.example.com/users"))
    repository.addEntry(createEntry(url = "https://api.example.com/products"))
    repository.addEntry(createEntry(url = "https://other.com/data"))

    val results = repository.searchLogs("users")
    assertEquals(1, results.size)
    assertTrue(results[0].url.contains("users"))
  }

  @Test
  fun `searchLogs is case insensitive`() {
    repository.addEntry(createEntry(url = "https://api.example.com/Users"))

    val results = repository.searchLogs("users")
    assertEquals(1, results.size)
  }

  @Test
  fun `searchLogs returns all for blank query`() {
    repeat(5) { repository.addEntry(createEntry()) }

    assertEquals(5, repository.searchLogs("").size)
    assertEquals(5, repository.searchLogs("   ").size)
  }

  @Test
  fun `searchLogs searches request body`() {
    repository.addEntry(createEntry(requestBody = """{"username": "john"}"""))
    repository.addEntry(createEntry(requestBody = """{"username": "jane"}"""))

    val results = repository.searchLogs("john")
    assertEquals(1, results.size)
  }

  // ── Filter by status ─────────────────────────────────────

  @Test
  fun `filterByStatus returns entries matching status type`() {
    repository.addEntry(createEntry(responseCode = 200, isSuccessful = true, isInProgress = false))
    repository.addEntry(createEntry(responseCode = 404, isInProgress = false))
    repository.addEntry(createEntry(responseCode = 500, isInProgress = false))
    repository.addEntry(createEntry(isInProgress = true))

    assertEquals(1, repository.filterByStatus(NetworkLogEntry.StatusType.SUCCESS).size)
    assertEquals(1, repository.filterByStatus(NetworkLogEntry.StatusType.CLIENT_ERROR).size)
    assertEquals(1, repository.filterByStatus(NetworkLogEntry.StatusType.SERVER_ERROR).size)
    assertEquals(1, repository.filterByStatus(NetworkLogEntry.StatusType.PENDING).size)
  }

  // ── Stats ────────────────────────────────────────────────

  @Test
  fun `getStats returns correct statistics`() {
    // 2 successful, 1 failed, 1 in-progress (excluded from stats)
    repository.addEntry(
        createEntry(
            responseCode = 200,
            isSuccessful = true,
            isInProgress = false,
            durationMs = 100,
            responseBodySize = 1024))
    repository.addEntry(
        createEntry(
            responseCode = 200,
            isSuccessful = true,
            isInProgress = false,
            durationMs = 200,
            responseBodySize = 2048))
    repository.addEntry(createEntry(responseCode = 500, isInProgress = false, durationMs = 50))
    repository.addEntry(createEntry(isInProgress = true))

    val stats = repository.getStats()
    assertEquals(3, stats.totalRequests) // excludes in-progress
    assertEquals(2, stats.successfulRequests)
    assertEquals(1, stats.failedRequests)
    assertEquals(116L, stats.averageDurationMs) // (100+200+50)/3 = 116
    assertEquals(3072L, stats.totalBytesReceived) // 1024+2048+0
  }

  @Test
  fun `getStats returns zeroes for empty repository`() {
    val stats = repository.getStats()
    assertEquals(0, stats.totalRequests)
    assertEquals(0, stats.successfulRequests)
    assertEquals(0, stats.failedRequests)
    assertEquals(0L, stats.averageDurationMs)
    assertEquals(0L, stats.totalBytesReceived)
  }

  // ── StateFlow emissions ──────────────────────────────────

  @Test
  fun `logs StateFlow updates on add`() {
    assertTrue(repository.logs.value.isEmpty())

    repository.addEntry(createEntry())
    assertEquals(1, repository.logs.value.size)

    repository.addEntry(createEntry())
    assertEquals(2, repository.logs.value.size)
  }

  @Test
  fun `logs StateFlow updates on clear`() {
    repository.addEntry(createEntry())
    assertEquals(1, repository.logs.value.size)

    repository.clear()
    assertTrue(repository.logs.value.isEmpty())
  }

  // ── Helpers ──────────────────────────────────────────────

  private fun createEntry(
      id: String = java.util.UUID.randomUUID().toString(),
      url: String = "https://example.com/api",
      method: String = "GET",
      responseCode: Int = 0,
      isSuccessful: Boolean = false,
      isInProgress: Boolean = true,
      durationMs: Long = 0,
      responseBodySize: Long = 0,
      requestBody: String? = null
  ) =
      NetworkLogEntry(
          id = id,
          method = method,
          url = url,
          host = "example.com",
          path = "/api",
          responseCode = responseCode,
          isSuccessful = isSuccessful,
          isInProgress = isInProgress,
          durationMs = durationMs,
          responseBodySize = responseBodySize,
          requestBody = requestBody)
}
