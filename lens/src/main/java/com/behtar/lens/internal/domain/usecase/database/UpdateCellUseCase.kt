package com.behtar.lens.internal.domain.usecase.database

import com.behtar.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for updating a cell value in the database.
 *
 * ## Debug Only
 * This operation is only available in debug builds. In release builds, it will always return
 * failure.
 *
 * Uses rowid to identify the specific row to update.
 */
class UpdateCellUseCase(private val repository: DatabaseRepository) {
  /**
   * Updates a cell value.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param rowId The SQLite rowid of the row
   * @param columnName The column to update
   * @param newValue The new value (null to set NULL)
   * @return Result indicating success or failure
   */
  suspend operator fun invoke(
      dbName: String,
      tableName: String,
      rowId: Long,
      columnName: String,
      newValue: String?
  ): Result<Unit> {
    return repository.updateCell(
        dbName = dbName,
        tableName = tableName,
        rowId = rowId,
        columnName = columnName,
        newValue = newValue)
  }
}
