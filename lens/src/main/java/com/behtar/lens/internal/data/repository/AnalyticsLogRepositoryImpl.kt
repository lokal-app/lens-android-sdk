package com.behtar.lens.internal.data.repository

import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.data.model.UserPropertyEntry
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of [AnalyticsLogRepository].
 *
 * Stores analytics events and user properties in thread-safe lists with automatic size limiting to
 * prevent memory issues.
 *
 * ## Memory Management
 * - Maximum 1000 events stored
 * - Maximum 100 user property entries stored
 * - Oldest entries are removed when limits are exceeded
 *
 * ## Thread Safety
 * Uses [CopyOnWriteArrayList] for thread-safe operations and [MutableStateFlow] for reactive
 * updates.
 */
class AnalyticsLogRepositoryImpl : AnalyticsLogRepository {

  private val events = CopyOnWriteArrayList<AnalyticsLogEntry>()
  private val userProperties = CopyOnWriteArrayList<UserPropertyEntry>()

  private val _eventsFlow = MutableStateFlow<List<AnalyticsLogEntry>>(emptyList())
  private val _userPropertiesFlow = MutableStateFlow<List<UserPropertyEntry>>(emptyList())

  override fun logEvent(entry: AnalyticsLogEntry) {
    // Add to beginning (newest first)
    events.add(0, entry)

    // Trim if over limit
    while (events.size > MAX_EVENTS) {
      events.removeAt(events.size - 1)
    }

    // Emit update
    _eventsFlow.update { events.toList() }

    // Debug logging
    timber.log.Timber.d(
        "AnalyticsLogRepository: Logged event '${entry.eventName}', total events: ${events.size}")
  }

  override fun logUserProperty(entry: UserPropertyEntry) {
    // Add to beginning (newest first)
    userProperties.add(0, entry)

    // Trim if over limit
    while (userProperties.size > MAX_USER_PROPERTIES) {
      userProperties.removeAt(userProperties.size - 1)
    }

    // Emit update
    _userPropertiesFlow.update { userProperties.toList() }
  }

  override fun getEvents(): Flow<List<AnalyticsLogEntry>> = _eventsFlow.asStateFlow()

  override fun getUserProperties(): Flow<List<UserPropertyEntry>> =
      _userPropertiesFlow.asStateFlow()

  override fun clear() {
    clearEvents()
    clearUserProperties()
  }

  override fun clearEvents() {
    events.clear()
    _eventsFlow.update { emptyList() }
  }

  override fun clearUserProperties() {
    userProperties.clear()
    _userPropertiesFlow.update { emptyList() }
  }

  companion object {
    /** Maximum number of events to store. */
    private const val MAX_EVENTS = 1000

    /** Maximum number of user property entries to store. */
    private const val MAX_USER_PROPERTIES = 100
  }
}
