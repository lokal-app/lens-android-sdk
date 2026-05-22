package com.lokalapps.lens.internal.domain.usecase.database

import com.lokalapps.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for deleting a row from the database.
 *
 * ## Debug Only
 * This operation is only available in debug builds. In release builds, it will always return
 * failure.
 *
 * Uses rowid to identify the specific row to delete.
 */
class DeleteRowUseCase(private val repository: DatabaseRepository) {
  /**
   * Deletes a row from the table.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param rowId The SQLite rowid of the row
   * @return Result indicating success or failure
   */
  suspend operator fun invoke(dbName: String, tableName: String, rowId: Long): Result<Unit> {
    return repository.deleteRow(dbName = dbName, tableName = tableName, rowId = rowId)
  }
}
