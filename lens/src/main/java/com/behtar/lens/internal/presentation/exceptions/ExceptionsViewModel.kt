package com.behtar.lens.internal.presentation.exceptions

import androidx.lifecycle.ViewModel
import com.behtar.lens.internal.data.model.ExceptionLogEntry
import com.behtar.lens.internal.domain.usecase.exceptions.ClearExceptionsUseCase
import com.behtar.lens.internal.domain.usecase.exceptions.GetExceptionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Exceptions screen.
 *
 * Follows MVI pattern:
 * - Model: [ExceptionsUiState]
 * - View: Compose screens
 * - Intent: [ExceptionsEvent]
 *
 * Dependencies are provided via [LensViewModelFactory].
 */
class ExceptionsViewModel(
    private val getExceptionsUseCase: GetExceptionsUseCase,
    private val clearExceptionsUseCase: ClearExceptionsUseCase
) : ViewModel() {

  private val _uiState = MutableStateFlow(ExceptionsUiState())
  val uiState: StateFlow<ExceptionsUiState> = _uiState.asStateFlow()

  /** Expose exceptions flow directly from repository */
  val exceptions: StateFlow<List<ExceptionLogEntry>> = getExceptionsUseCase()

  /** Single entry point for all user events. */
  fun onEvent(event: ExceptionsEvent) {
    when (event) {
      is ExceptionsEvent.SelectEntry -> selectEntry(event.entry)
      ExceptionsEvent.NavigateBack -> navigateBack()
      ExceptionsEvent.ClearExceptions -> clearExceptions()
    }
  }

  private fun selectEntry(entry: ExceptionLogEntry) {
    _uiState.update { it.copy(selectedEntry = entry) }
  }

  private fun navigateBack() {
    _uiState.update { it.copy(selectedEntry = null) }
  }

  private fun clearExceptions() {
    clearExceptionsUseCase()
  }
}
