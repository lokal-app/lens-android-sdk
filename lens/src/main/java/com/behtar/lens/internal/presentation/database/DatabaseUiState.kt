package com.behtar.lens.internal.presentation.database

import com.behtar.lens.internal.data.model.ColumnInfo
import com.behtar.lens.internal.data.model.DatabaseInfo
import com.behtar.lens.internal.data.model.QueryResult
import com.behtar.lens.internal.data.model.TableInfo

/**
 * UI state for the Database Inspector screen.
 *
 * Uses MVI pattern with immutable state and unidirectional data flow. State updates are emitted via
 * StateFlow and UI reacts to changes.
 *
 * @property currentScreen Current navigation state within Database Inspector
 * @property isLoading Whether a database operation is in progress
 * @property error Error message to display (null if no error)
 * @property databases List of all databases (populated on DatabaseList screen)
 * @property selectedDatabase Currently selected database
 * @property tables Tables in the selected database
 * @property selectedTable Currently selected table
 * @property tableSchema Schema columns for the selected table
 * @property queryResult Current query result (table data or custom query)
 * @property customQuery Custom SQL query in the editor
 * @property currentPage Current pagination page
 * @property pageSize Number of rows per page
 * @property sortColumn Column to sort by (null for default order)
 * @property sortAscending Sort direction
 * @property includeSystemTables Whether to show sqlite_* tables
 */
data class DatabaseUiState(
    val currentScreen: DatabaseScreen = DatabaseScreen.DatabaseList,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Data state
    val databases: List<DatabaseInfo> = emptyList(),
    val selectedDatabase: DatabaseInfo? = null,
    val tables: List<TableInfo> = emptyList(),
    val selectedTable: TableInfo? = null,
    val tableSchema: List<ColumnInfo> = emptyList(),
    val queryResult: QueryResult = QueryResult.empty(),

    // Query editor state
    val customQuery: String = "",

    // Pagination state
    val currentPage: Int = 0,
    val pageSize: Int = 50,

    // Sorting state
    val sortColumn: String? = null,
    val sortAscending: Boolean = true,

    // Filter state
    val includeSystemTables: Boolean = false
)

/** Navigation screens within the Database Inspector. */
sealed class DatabaseScreen {
  /** List of all databases in the app. */
  data object DatabaseList : DatabaseScreen()

  /** List of tables in a selected database. */
  data class TableList(val dbName: String) : DatabaseScreen()

  /** Table data view with pagination. */
  data class TableData(val dbName: String, val tableName: String) : DatabaseScreen()

  /** Table schema/column information. */
  data class TableSchema(val dbName: String, val tableName: String) : DatabaseScreen()

  /** Custom SQL query editor. */
  data class QueryEditor(val dbName: String) : DatabaseScreen()
}

/**
 * Events that can be triggered from the Database Inspector UI.
 *
 * Following MVI pattern, these events are dispatched to the ViewModel which processes them and
 * updates the state accordingly.
 */
sealed class DatabaseEvent {
  /** Refresh the list of databases. */
  data object RefreshDatabases : DatabaseEvent()

  /** Select a database to view its tables. */
  data class SelectDatabase(val database: DatabaseInfo) : DatabaseEvent()

  /** Select a table to view its data. */
  data class SelectTable(val table: TableInfo) : DatabaseEvent()

  /** View schema for the currently selected table. */
  data object ViewSchema : DatabaseEvent()

  /** Open query editor for the selected database. */
  data object OpenQueryEditor : DatabaseEvent()

  /** Update the custom query text. */
  data class UpdateQuery(val query: String) : DatabaseEvent()

  /** Execute the custom query. */
  data object ExecuteQuery : DatabaseEvent()

  /** Navigate to a specific page of results. */
  data class LoadPage(val page: Int) : DatabaseEvent()

  /** Sort table by a column. */
  data class SortBy(val columnName: String) : DatabaseEvent()

  /** Update a cell value (debug only). */
  data class UpdateCell(val rowId: Long, val columnName: String, val newValue: String?) :
      DatabaseEvent()

  /** Delete a row (debug only). */
  data class DeleteRow(val rowId: Long) : DatabaseEvent()

  /** Navigate back to previous screen. */
  data object NavigateBack : DatabaseEvent()

  /** Clear current error message. */
  data object ClearError : DatabaseEvent()

  /** Toggle inclusion of system tables. */
  data object ToggleSystemTables : DatabaseEvent()

  /** Refresh current data (tables or query result). */
  data object Refresh : DatabaseEvent()
}
