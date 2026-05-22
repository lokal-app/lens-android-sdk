package com.lokalapps.lens.internal.presentation.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lokalapps.lens.internal.data.model.DatabaseInfo
import com.lokalapps.lens.internal.data.model.TableInfo
import com.lokalapps.lens.internal.domain.usecase.database.DeleteRowUseCase
import com.lokalapps.lens.internal.domain.usecase.database.ExecuteQueryUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetDatabasesUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetTableSchemaUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetTablesUseCase
import com.lokalapps.lens.internal.domain.usecase.database.QueryTableUseCase
import com.lokalapps.lens.internal.domain.usecase.database.UpdateCellUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Database Inspector screen.
 *
 * Implements MVI pattern with:
 * - [DatabaseUiState] for immutable UI state
 * - [DatabaseEvent] for user actions
 * - [onEvent] for processing events
 *
 * ## Architecture
 * Uses use cases from the domain layer for all database operations. State is managed via
 * [StateFlow] for reactive UI updates.
 *
 * ## Error Handling
 * All database errors are caught and displayed via the error state. Critical errors are also logged
 * to Timber.
 *
 * Dependencies are provided via [LensViewModelFactory].
 */
class DatabaseViewModel(
    private val getDatabasesUseCase: GetDatabasesUseCase,
    private val getTablesUseCase: GetTablesUseCase,
    private val getTableSchemaUseCase: GetTableSchemaUseCase,
    private val queryTableUseCase: QueryTableUseCase,
    private val executeQueryUseCase: ExecuteQueryUseCase,
    private val updateCellUseCase: UpdateCellUseCase,
    private val deleteRowUseCase: DeleteRowUseCase
) : ViewModel() {

  private val _uiState = MutableStateFlow(DatabaseUiState())
  val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()

  init {
    // Load databases on initialization
    loadDatabases()
  }

  /**
   * Processes UI events following MVI pattern.
   *
   * Each event maps to a specific state change or side effect.
   */
  fun onEvent(event: DatabaseEvent) {
    when (event) {
      is DatabaseEvent.RefreshDatabases -> loadDatabases()
      is DatabaseEvent.SelectDatabase -> selectDatabase(event.database)
      is DatabaseEvent.SelectTable -> selectTable(event.table)
      is DatabaseEvent.ViewSchema -> viewSchema()
      is DatabaseEvent.OpenQueryEditor -> openQueryEditor()
      is DatabaseEvent.UpdateQuery -> updateQuery(event.query)
      is DatabaseEvent.ExecuteQuery -> executeQuery()
      is DatabaseEvent.LoadPage -> loadPage(event.page)
      is DatabaseEvent.SortBy -> sortBy(event.columnName)
      is DatabaseEvent.UpdateCell -> updateCell(event.rowId, event.columnName, event.newValue)
      is DatabaseEvent.DeleteRow -> deleteRow(event.rowId)
      is DatabaseEvent.NavigateBack -> navigateBack()
      is DatabaseEvent.ClearError -> clearError()
      is DatabaseEvent.ToggleSystemTables -> toggleSystemTables()
      is DatabaseEvent.Refresh -> refresh()
    }
  }

  // ─────────────────────────────────────────────────────────────────────
  // Event Handlers
  // ─────────────────────────────────────────────────────────────────────

  private fun loadDatabases() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val databases = getDatabasesUseCase()
        _uiState.update {
          it.copy(
              isLoading = false, databases = databases, currentScreen = DatabaseScreen.DatabaseList)
        }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to load databases")
        _uiState.update {
          it.copy(isLoading = false, error = "Failed to load databases: ${e.message}")
        }
      }
    }
  }

  private fun selectDatabase(database: DatabaseInfo) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null, selectedDatabase = database) }
      try {
        val tables =
            getTablesUseCase(
                dbName = database.name, includeSystemTables = _uiState.value.includeSystemTables)
        _uiState.update {
          it.copy(
              isLoading = false,
              tables = tables,
              currentScreen = DatabaseScreen.TableList(database.name))
        }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to load tables")
        _uiState.update {
          it.copy(isLoading = false, error = "Failed to load tables: ${e.message}")
        }
      }
    }
  }

  private fun selectTable(table: TableInfo) {
    val dbName = _uiState.value.selectedDatabase?.name ?: return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
            isLoading = true,
            error = null,
            selectedTable = table,
            currentPage = 0,
            sortColumn = null,
            sortAscending = true)
      }
      try {
        val result =
            queryTableUseCase(
                dbName = dbName,
                tableName = table.name,
                pageSize = _uiState.value.pageSize,
                page = 0)
        _uiState.update {
          it.copy(
              isLoading = false,
              queryResult = result,
              currentScreen = DatabaseScreen.TableData(dbName, table.name))
        }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to query table")
        _uiState.update {
          it.copy(isLoading = false, error = "Failed to query table: ${e.message}")
        }
      }
    }
  }

  private fun viewSchema() {
    val state = _uiState.value
    val dbName = state.selectedDatabase?.name ?: return
    val tableName = state.selectedTable?.name ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val schema = getTableSchemaUseCase(dbName, tableName)
        _uiState.update {
          it.copy(
              isLoading = false,
              tableSchema = schema,
              currentScreen = DatabaseScreen.TableSchema(dbName, tableName))
        }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to load schema")
        _uiState.update {
          it.copy(isLoading = false, error = "Failed to load schema: ${e.message}")
        }
      }
    }
  }

  private fun openQueryEditor() {
    val dbName = _uiState.value.selectedDatabase?.name ?: return
    _uiState.update {
      it.copy(
          currentScreen = DatabaseScreen.QueryEditor(dbName),
          customQuery =
              if (it.customQuery.isBlank()) {
                "SELECT * FROM table_name LIMIT 50"
              } else {
                it.customQuery
              })
    }
  }

  private fun updateQuery(query: String) {
    _uiState.update { it.copy(customQuery = query) }
  }

  private fun executeQuery() {
    val state = _uiState.value
    val dbName = state.selectedDatabase?.name ?: return
    val query = state.customQuery.trim()

    if (query.isBlank()) {
      _uiState.update { it.copy(error = "Query cannot be empty") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val result = executeQueryUseCase(dbName, query)
        _uiState.update { it.copy(isLoading = false, queryResult = result, error = result.error) }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Query execution failed")
        _uiState.update { it.copy(isLoading = false, error = "Query failed: ${e.message}") }
      }
    }
  }

  private fun loadPage(page: Int) {
    val state = _uiState.value
    val dbName = state.selectedDatabase?.name ?: return
    val tableName = state.selectedTable?.name ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val result =
            queryTableUseCase(
                dbName = dbName,
                tableName = tableName,
                pageSize = state.pageSize,
                page = page,
                orderBy = state.sortColumn,
                ascending = state.sortAscending)
        _uiState.update { it.copy(isLoading = false, queryResult = result, currentPage = page) }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to load page")
        _uiState.update { it.copy(isLoading = false, error = "Failed to load page: ${e.message}") }
      }
    }
  }

  private fun sortBy(columnName: String) {
    val state = _uiState.value

    // Toggle direction if clicking same column
    val ascending =
        if (state.sortColumn == columnName) {
          !state.sortAscending
        } else {
          true
        }

    _uiState.update { it.copy(sortColumn = columnName, sortAscending = ascending, currentPage = 0) }

    // Reload data with new sort
    val dbName = state.selectedDatabase?.name ?: return
    val tableName = state.selectedTable?.name ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val result =
            queryTableUseCase(
                dbName = dbName,
                tableName = tableName,
                pageSize = state.pageSize,
                page = 0,
                orderBy = columnName,
                ascending = ascending)
        _uiState.update { it.copy(isLoading = false, queryResult = result) }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to sort")
        _uiState.update { it.copy(isLoading = false, error = "Failed to sort: ${e.message}") }
      }
    }
  }

  private fun updateCell(rowId: Long, columnName: String, newValue: String?) {
    val state = _uiState.value
    val dbName = state.selectedDatabase?.name ?: return
    val tableName = state.selectedTable?.name ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        updateCellUseCase(dbName, tableName, rowId, columnName, newValue)
            .onSuccess {
              // Refresh current page
              refresh()
            }
            .onFailure { e ->
              _uiState.update {
                it.copy(isLoading = false, error = "Failed to update cell: ${e.message}")
              }
            }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to update cell")
        _uiState.update {
          it.copy(isLoading = false, error = "Failed to update cell: ${e.message}")
        }
      }
    }
  }

  private fun deleteRow(rowId: Long) {
    val state = _uiState.value
    val dbName = state.selectedDatabase?.name ?: return
    val tableName = state.selectedTable?.name ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        deleteRowUseCase(dbName, tableName, rowId)
            .onSuccess {
              // Refresh current page
              refresh()
            }
            .onFailure { e ->
              _uiState.update {
                it.copy(isLoading = false, error = "Failed to delete row: ${e.message}")
              }
            }
      } catch (e: Exception) {
        Timber.e(e, "DatabaseViewModel: Failed to delete row")
        _uiState.update { it.copy(isLoading = false, error = "Failed to delete row: ${e.message}") }
      }
    }
  }

  private fun navigateBack() {
    val state = _uiState.value

    when (state.currentScreen) {
      is DatabaseScreen.DatabaseList -> {
        // Already at root, do nothing
      }
      is DatabaseScreen.TableList -> {
        // Go back to database list
        _uiState.update {
          it.copy(
              currentScreen = DatabaseScreen.DatabaseList,
              selectedDatabase = null,
              tables = emptyList())
        }
      }
      is DatabaseScreen.TableData,
      is DatabaseScreen.TableSchema -> {
        // Go back to table list
        val dbName = state.selectedDatabase?.name ?: return
        _uiState.update {
          it.copy(
              currentScreen = DatabaseScreen.TableList(dbName),
              selectedTable = null,
              queryResult = com.lokalapps.lens.internal.data.model.QueryResult.empty(),
              tableSchema = emptyList())
        }
      }
      is DatabaseScreen.QueryEditor -> {
        // Go back to table list
        val dbName = state.selectedDatabase?.name ?: return
        _uiState.update {
          it.copy(
              currentScreen = DatabaseScreen.TableList(dbName),
              queryResult = com.lokalapps.lens.internal.data.model.QueryResult.empty())
        }
      }
    }
  }

  private fun clearError() {
    _uiState.update { it.copy(error = null) }
  }

  private fun toggleSystemTables() {
    val state = _uiState.value
    val newValue = !state.includeSystemTables

    _uiState.update { it.copy(includeSystemTables = newValue) }

    // Reload tables if on table list screen
    if (state.currentScreen is DatabaseScreen.TableList) {
      state.selectedDatabase?.let { selectDatabase(it) }
    }
  }

  private fun refresh() {
    val state = _uiState.value

    when (val screen = state.currentScreen) {
      is DatabaseScreen.DatabaseList -> loadDatabases()
      is DatabaseScreen.TableList -> {
        state.selectedDatabase?.let { selectDatabase(it) }
      }
      is DatabaseScreen.TableData -> {
        loadPage(state.currentPage)
      }
      is DatabaseScreen.TableSchema -> viewSchema()
      is DatabaseScreen.QueryEditor -> {
        // Don't auto-refresh query results
      }
    }
  }
}
