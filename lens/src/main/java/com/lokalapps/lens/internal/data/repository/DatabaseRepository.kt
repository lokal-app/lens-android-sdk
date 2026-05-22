package com.lokalapps.lens.internal.data.repository

import com.lokalapps.lens.internal.data.model.ColumnInfo
import com.lokalapps.lens.internal.data.model.DatabaseInfo
import com.lokalapps.lens.internal.data.model.QueryResult
import com.lokalapps.lens.internal.data.model.TableInfo

/**
 * Repository interface for SQLite database inspection operations.
 *
 * Provides methods to:
 * - Discover all databases in the app
 * - Inspect table schemas
 * - Query table data with pagination
 * - Execute custom SQL queries
 * - Modify data (debug builds only)
 *
 * All operations are suspend functions designed to run on IO dispatcher.
 */
interface DatabaseRepository {

  /**
   * Discovers all SQLite databases in the application.
   *
   * This includes both Room-managed databases and raw SQLite databases. System databases (like
   * WebView storage) may also be included.
   *
   * @return List of database info, sorted by name
   */
  suspend fun getDatabases(): List<DatabaseInfo>

  /**
   * Gets all user tables in the specified database.
   *
   * Excludes system tables (sqlite_*) by default.
   *
   * @param dbName The database filename
   * @param includeSystemTables Whether to include sqlite_* tables
   * @return List of table info, sorted by name
   */
  suspend fun getTables(dbName: String, includeSystemTables: Boolean = false): List<TableInfo>

  /**
   * Gets detailed schema information for a table.
   *
   * Uses PRAGMA table_info() to retrieve column details.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @return List of column info in column order
   */
  suspend fun getTableSchema(dbName: String, tableName: String): List<ColumnInfo>

  /**
   * Queries table data with pagination support.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param pageSize Number of rows per page (default 50)
   * @param page Page index (0-based)
   * @param orderBy Column name to sort by (null for default order)
   * @param ascending Sort direction (true = ASC, false = DESC)
   * @return Query result with rows and pagination info
   */
  suspend fun queryTable(
      dbName: String,
      tableName: String,
      pageSize: Int = 50,
      page: Int = 0,
      orderBy: String? = null,
      ascending: Boolean = true
  ): QueryResult

  /**
   * Executes a custom SQL query.
   *
   * For SELECT queries, returns result data. For other queries, returns affected row count.
   *
   * @param dbName The database filename
   * @param query The SQL query to execute
   * @return Query result (may contain error if query failed)
   */
  suspend fun executeQuery(dbName: String, query: String): QueryResult

  /**
   * Updates a cell value in the database.
   *
   * Only available in debug builds. Uses rowid to identify the row.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param rowId The SQLite rowid of the row to update
   * @param columnName The column to update
   * @param newValue The new value (null to set NULL)
   * @return Result indicating success or failure with error message
   */
  suspend fun updateCell(
      dbName: String,
      tableName: String,
      rowId: Long,
      columnName: String,
      newValue: String?
  ): Result<Unit>

  /**
   * Deletes a row from the database.
   *
   * Only available in debug builds. Uses rowid to identify the row.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param rowId The SQLite rowid of the row to delete
   * @return Result indicating success or failure with error message
   */
  suspend fun deleteRow(dbName: String, tableName: String, rowId: Long): Result<Unit>
}
