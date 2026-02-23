package com.behtar.lens.internal.presentation.analytics

import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.data.model.UserPropertyEntry

/**
 * UI state for the Analytics Inspector screen.
 *
 * Uses MVI pattern with immutable state and unidirectional data flow.
 *
 * @property currentTab Currently selected tab (Events or User Properties)
 * @property events List of logged analytics events
 * @property userProperties List of logged user property updates
 * @property selectedEvent Currently selected event for detail view (null if none)
 * @property searchQuery Current search filter text
 * @property isLoading Whether data is loading
 */
data class AnalyticsUiState(
    val currentTab: AnalyticsTab = AnalyticsTab.Events,
    val events: List<AnalyticsLogEntry> = emptyList(),
    val userProperties: List<UserPropertyEntry> = emptyList(),
    val selectedEvent: AnalyticsLogEntry? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
  /** Events filtered by search query. */
  val filteredEvents: List<AnalyticsLogEntry>
    get() =
        if (searchQuery.isBlank()) {
          events
        } else {
          events.filter { event -> matchesSearch(event) }
        }

  /** User properties filtered by search query. */
  val filteredUserProperties: List<UserPropertyEntry>
    get() =
        if (searchQuery.isBlank()) {
          userProperties
        } else {
          userProperties.filter { property -> matchesPropertySearch(property) }
        }

  private fun matchesSearch(event: AnalyticsLogEntry): Boolean {
    val query = searchQuery.lowercase()
    return event.eventName.lowercase().contains(query) ||
        event.params.any { (key, value) ->
          key.lowercase().contains(query) || value?.toString()?.lowercase()?.contains(query) == true
        }
  }

  private fun matchesPropertySearch(property: UserPropertyEntry): Boolean {
    val query = searchQuery.lowercase()
    return property.userId?.lowercase()?.contains(query) == true ||
        property.properties.any { (key, value) ->
          key.lowercase().contains(query) || value?.toString()?.lowercase()?.contains(query) == true
        }
  }
}

/** Tabs in the Analytics Inspector. */
enum class AnalyticsTab {
  /** Shows analytics events. */
  Events,

  /** Shows user property updates. */
  UserProperties
}

/** Events that can be triggered from the Analytics Inspector UI. */
sealed class AnalyticsEvent {
  /** Switch to a different tab. */
  data class SelectTab(val tab: AnalyticsTab) : AnalyticsEvent()

  /** Select an event to view details. */
  data class SelectEvent(val event: AnalyticsLogEntry?) : AnalyticsEvent()

  /** Update the search query. */
  data class UpdateSearch(val query: String) : AnalyticsEvent()

  /** Clear all logs. */
  data object ClearLogs : AnalyticsEvent()

  /** Clear only event logs. */
  data object ClearEvents : AnalyticsEvent()

  /** Clear only user property logs. */
  data object ClearUserProperties : AnalyticsEvent()
}
