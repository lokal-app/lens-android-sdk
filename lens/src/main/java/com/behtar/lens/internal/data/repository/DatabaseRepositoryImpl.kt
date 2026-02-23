package com.behtar.lens.internal.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.behtar.lens.BuildConfig
import com.behtar.lens.internal.data.model.ColumnInfo
import com.behtar.lens.internal.data.model.DatabaseInfo
import com.behtar.lens.internal.data.model.QueryResult
import com.behtar.lens.internal.data.model.TableInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementation of [DatabaseRepository] using Android's SQLite APIs.
 *
 * This class provides read access to all SQLite databases in the app, with write access restricted
 * to debug builds.
 *
 * ## Thread Safety
 * All database operations run on [Dispatchers.IO] and use try-finally blocks to ensure proper
 * cursor and database cleanup.
 *
 * ## Security
 * - Databases are opened read-only by default
 * - Write operations only work when [BuildConfig.DEBUG] is true
 * - Table/column names are escaped to prevent SQL injection
 */
class DatabaseRepositoryImpl(private val context: Context) : DatabaseRepository {

  override suspend fun getDatabases(): List<DatabaseInfo> =
      withContext(Dispatchers.IO) {
        val databases = mutableListOf<DatabaseInfo>()

        try {
          context.databaseList().forEach { dbName ->
            try {
              val dbPath = context.getDatabasePath(dbName)
              if (dbPath.exists() && dbPath.isFile) {
                val db = openDatabase(dbName, readOnly = true)
                try {
                  val tableCount = countUserTables(db)
                  val isRoom = checkIsRoomDatabase(db)

                  databases.add(
                      DatabaseInfo(
                          name = dbName,
                          path = dbPath.absolutePath,
                          sizeBytes = dbPath.length(),
                          tableCount = tableCount,
                          isRoomDatabase = isRoom))
                } finally {
                  db.close()
                }
              }
            } catch (e: Exception) {
              Timber.w(e, "DatabaseRepository: Could not inspect database '$dbName'")
            }
          }
        } catch (e: Exception) {
          Timber.e(e, "DatabaseRepository: Failed to list databases")
        }

        databases.sortedBy { it.name }
      }

  override suspend fun getTables(dbName: String, includeSystemTables: Boolean): List<TableInfo> =
      withContext(Dispatchers.IO) {
        val tables = mutableListOf<TableInfo>()

        val db = openDatabase(dbName, readOnly = true)
        try {
          // Query sqlite_master for table names
          val query =
              if (includeSystemTables) {
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
              } else {
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' ORDER BY name"
              }

          db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
              val tableName = cursor.getString(0)
              try {
                val rowCount = getTableRowCount(db, tableName)
                val columnCount = getTableColumnCount(db, tableName)

                tables.add(
                    TableInfo(name = tableName, rowCount = rowCount, columnCount = columnCount))
              } catch (e: Exception) {
                Timber.w(e, "DatabaseRepository: Could not get info for table '$tableName'")
              }
            }
          }
        } finally {
          db.close()
        }

        tables
      }

  override suspend fun getTableSchema(dbName: String, tableName: String): List<ColumnInfo> =
      withContext(Dispatchers.IO) {
        val columns = mutableListOf<ColumnInfo>()

        val db = openDatabase(dbName, readOnly = true)
        try {
          // PRAGMA table_info returns: cid, name, type, notnull, dflt_value, pk
          db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            while (cursor.moveToNext()) {
              columns.add(
                  ColumnInfo(
                      position = cursor.getInt(0),
                      name = cursor.getString(1),
                      type = cursor.getString(2) ?: "",
                      isNotNull = cursor.getInt(3) == 1,
                      defaultValue = cursor.getStringOrNull(4),
                      isPrimaryKey = cursor.getInt(5) > 0))
            }
          }
        } finally {
          db.close()
        }

        columns
      }

  override suspend fun queryTable(
      dbName: String,
      tableName: String,
      pageSize: Int,
      page: Int,
      orderBy: String?,
      ascending: Boolean
  ): QueryResult =
      withContext(Dispatchers.IO) {
        val db = openDatabase(dbName, readOnly = true)
        try {
          val startTime = System.currentTimeMillis()

          // Get total count
          val totalRows = getTableRowCount(db, tableName)

          // Build query with pagination
          val orderClause =
              orderBy?.let { "ORDER BY `$it` ${if (ascending) "ASC" else "DESC"}" } ?: ""
          val offset = page * pageSize
          val query =
              "SELECT rowid, * FROM `$tableName` $orderClause LIMIT $pageSize OFFSET $offset"

          val (columns, rows) = executeSelectQuery(db, query)

          QueryResult(
              columns = columns,
              rows = rows,
              rowCount = rows.size,
              totalRows = totalRows,
              currentPage = page,
              hasMore = (page + 1) * pageSize < totalRows,
              executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
          Timber.e(e, "DatabaseRepository: Failed to query table '$tableName'")
          QueryResult.error(e.message ?: "Query failed")
        } finally {
          db.close()
        }
      }

  override suspend fun executeQuery(dbName: String, query: String): QueryResult =
      withContext(Dispatchers.IO) {
        val db = openDatabase(dbName, readOnly = isSelectQuery(query))
        try {
          val startTime = System.currentTimeMillis()
          val trimmedQuery = query.trim()

          // Block dangerous queries in non-debug builds
          if (!BuildConfig.DEBUG && !isSelectQuery(trimmedQuery)) {
            return@withContext QueryResult.error("Write queries are only allowed in debug builds")
          }

          // Block destructive queries without WHERE clause
          if (isDestructiveWithoutWhere(trimmedQuery)) {
            return@withContext QueryResult.error(
                "Destructive queries without WHERE clause are not allowed")
          }

          if (isSelectQuery(trimmedQuery)) {
            val (columns, rows) = executeSelectQuery(db, trimmedQuery)
            QueryResult(
                columns = columns,
                rows = rows,
                rowCount = rows.size,
                totalRows = rows.size.toLong(),
                currentPage = 0,
                hasMore = false,
                executionTimeMs = System.currentTimeMillis() - startTime)
          } else {
            db.execSQL(trimmedQuery)
            QueryResult(
                columns = listOf("result"),
                rows = listOf(mapOf("result" to "Query executed successfully")),
                rowCount = 1,
                totalRows = 1,
                currentPage = 0,
                hasMore = false,
                executionTimeMs = System.currentTimeMillis() - startTime)
          }
        } catch (e: Exception) {
          Timber.e(e, "DatabaseRepository: Query failed: $query")
          QueryResult.error(e.message ?: "Query execution failed")
        } finally {
          db.close()
        }
      }

  override suspend fun updateCell(
      dbName: String,
      tableName: String,
      rowId: Long,
      columnName: String,
      newValue: String?
  ): Result<Unit> =
      withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
          return@withContext Result.failure(
              IllegalStateException("Cell updates are only allowed in debug builds"))
        }

        try {
          val db = openDatabase(dbName, readOnly = false)
          try {
            val value = if (newValue == null) "NULL" else "'${escapeSql(newValue)}'"
            db.execSQL("UPDATE `$tableName` SET `$columnName` = $value WHERE rowid = $rowId")
            Result.success(Unit)
          } finally {
            db.close()
          }
        } catch (e: Exception) {
          Timber.e(e, "DatabaseRepository: Failed to update cell")
          Result.failure(e)
        }
      }

  override suspend fun deleteRow(dbName: String, tableName: String, rowId: Long): Result<Unit> =
      withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
          return@withContext Result.failure(
              IllegalStateException("Row deletion is only allowed in debug builds"))
        }

        try {
          val db = openDatabase(dbName, readOnly = false)
          try {
            db.execSQL("DELETE FROM `$tableName` WHERE rowid = $rowId")
            Result.success(Unit)
          } finally {
            db.close()
          }
        } catch (e: Exception) {
          Timber.e(e, "DatabaseRepository: Failed to delete row")
          Result.failure(e)
        }
      }

  // ─────────────────────────────────────────────────────────────────────
  // Private Helper Methods
  // ─────────────────────────────────────────────────────────────────────

  /**
   * Opens a database by name.
   *
   * @param dbName The database filename
   * @param readOnly Whether to open in read-only mode
   * @return Opened SQLiteDatabase
   */
  private fun openDatabase(dbName: String, readOnly: Boolean): SQLiteDatabase {
    val dbPath = context.getDatabasePath(dbName)
    val flags =
        if (readOnly) {
          SQLiteDatabase.OPEN_READONLY
        } else {
          SQLiteDatabase.OPEN_READWRITE
        }
    return SQLiteDatabase.openDatabase(dbPath.absolutePath, null, flags)
  }

  /** Counts user tables (excluding sqlite_* and android_* tables). */
  private fun countUserTables(db: SQLiteDatabase): Int {
    return db.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
            null)
        .use { cursor ->
          cursor.moveToFirst()
          cursor.getInt(0)
        }
  }

  /** Checks if this is a Room database by looking for room_master_table. */
  private fun checkIsRoomDatabase(db: SQLiteDatabase): Boolean {
    return db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'", null)
        .use { cursor -> cursor.moveToFirst() }
  }

  /** Gets the row count for a table. */
  private fun getTableRowCount(db: SQLiteDatabase, tableName: String): Long {
    return db.rawQuery("SELECT COUNT(*) FROM `$tableName`", null).use { cursor ->
      cursor.moveToFirst()
      cursor.getLong(0)
    }
  }

  /** Gets the column count for a table. */
  private fun getTableColumnCount(db: SQLiteDatabase, tableName: String): Int {
    return db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor -> cursor.count }
  }

  /** Executes a SELECT query and returns columns and rows. */
  private fun executeSelectQuery(
      db: SQLiteDatabase,
      query: String
  ): Pair<List<String>, List<Map<String, Any?>>> {
    val columns = mutableListOf<String>()
    val rows = mutableListOf<Map<String, Any?>>()

    db.rawQuery(query, null).use { cursor ->
      columns.addAll(cursor.columnNames)

      while (cursor.moveToNext()) {
        val row = mutableMapOf<String, Any?>()
        for (i in 0 until cursor.columnCount) {
          row[cursor.getColumnName(i)] = cursor.getValueAt(i)
        }
        rows.add(row)
      }
    }

    return columns to rows
  }

  /** Gets the value at a column index, handling different types. */
  private fun Cursor.getValueAt(index: Int): Any? {
    return when (getType(index)) {
      Cursor.FIELD_TYPE_NULL -> null
      Cursor.FIELD_TYPE_INTEGER -> getLong(index)
      Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
      Cursor.FIELD_TYPE_BLOB -> "[BLOB: ${getBlob(index).size} bytes]"
      else -> getString(index)
    }
  }

  /** Extension function to get nullable string from cursor. */
  private fun Cursor.getStringOrNull(index: Int): String? {
    return if (isNull(index)) null else getString(index)
  }

  /** Checks if a query is a SELECT query. */
  private fun isSelectQuery(query: String): Boolean {
    val normalized = query.trim().uppercase()
    return normalized.startsWith("SELECT") ||
        normalized.startsWith("PRAGMA") ||
        normalized.startsWith("EXPLAIN")
  }

  /** Checks if a query is destructive without a WHERE clause. */
  private fun isDestructiveWithoutWhere(query: String): Boolean {
    val normalized = query.trim().uppercase()

    // DROP and TRUNCATE are always blocked
    if (normalized.startsWith("DROP") || normalized.startsWith("TRUNCATE")) {
      return true
    }

    // DELETE and UPDATE require WHERE clause
    if (normalized.startsWith("DELETE") || normalized.startsWith("UPDATE")) {
      return !normalized.contains("WHERE")
    }

    return false
  }

  /** Escapes single quotes in SQL strings. */
  private fun escapeSql(value: String): String {
    return value.replace("'", "''")
  }
}
