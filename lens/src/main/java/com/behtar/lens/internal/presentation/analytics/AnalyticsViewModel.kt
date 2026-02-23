package com.behtar.lens.internal.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.domain.usecase.analytics.ClearAnalyticsLogsUseCase
import com.behtar.lens.internal.domain.usecase.analytics.GetAnalyticsLogsUseCase
import com.behtar.lens.internal.domain.usecase.analytics.GetUserPropertiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Analytics Inspector screen.
 *
 * Implements MVI pattern with:
 * - [AnalyticsUiState] for immutable UI state
 * - [AnalyticsEvent] for user actions
 * - [onEvent] for processing events
 *
 * ## Data Flow
 * Collects events and user properties from their respective flows and combines them into the UI
 * state for reactive updates.
 *
 * Dependencies are provided via [LensViewModelFactory].
 */
class AnalyticsViewModel(
    private val getAnalyticsLogsUseCase: GetAnalyticsLogsUseCase,
    private val getUserPropertiesUseCase: GetUserPropertiesUseCase,
    private val clearAnalyticsLogsUseCase: ClearAnalyticsLogsUseCase
) : ViewModel() {

  private val _uiState = MutableStateFlow(AnalyticsUiState())
  val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

  init {
    // Collect analytics events
    viewModelScope.launch {
      getAnalyticsLogsUseCase().collect { events -> _uiState.update { it.copy(events = events) } }
    }

    // Collect user properties
    viewModelScope.launch {
      getUserPropertiesUseCase().collect { properties ->
        _uiState.update { it.copy(userProperties = properties) }
      }
    }
  }

  /** Processes UI events following MVI pattern. */
  fun onEvent(event: AnalyticsEvent) {
    when (event) {
      is AnalyticsEvent.SelectTab -> selectTab(event.tab)
      is AnalyticsEvent.SelectEvent -> selectEvent(event.event)
      is AnalyticsEvent.UpdateSearch -> updateSearch(event.query)
      is AnalyticsEvent.ClearLogs -> clearLogs()
      is AnalyticsEvent.ClearEvents -> clearEvents()
      is AnalyticsEvent.ClearUserProperties -> clearUserProperties()
    }
  }

  private fun selectTab(tab: AnalyticsTab) {
    _uiState.update { it.copy(currentTab = tab, selectedEvent = null) }
  }

  private fun selectEvent(event: AnalyticsLogEntry?) {
    _uiState.update { it.copy(selectedEvent = event) }
  }

  private fun updateSearch(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  private fun clearLogs() {
    clearAnalyticsLogsUseCase()
  }

  private fun clearEvents() {
    clearAnalyticsLogsUseCase.clearEvents()
  }

  private fun clearUserProperties() {
    clearAnalyticsLogsUseCase.clearUserProperties()
  }
}
