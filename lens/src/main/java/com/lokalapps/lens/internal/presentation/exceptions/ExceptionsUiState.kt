package com.lokalapps.lens.internal.presentation.exceptions

import com.lokalapps.lens.internal.data.model.ExceptionLogEntry

/** UI state for the Exceptions screen. */
data class ExceptionsUiState(val selectedEntry: ExceptionLogEntry? = null)

/** Sealed class representing all possible user events in the Exceptions screen. */
sealed class ExceptionsEvent {
  data class SelectEntry(val entry: ExceptionLogEntry) : ExceptionsEvent()

  data object NavigateBack : ExceptionsEvent()

  data object ClearExceptions : ExceptionsEvent()
}
