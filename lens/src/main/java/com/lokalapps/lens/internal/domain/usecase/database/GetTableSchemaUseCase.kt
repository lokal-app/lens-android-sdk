package com.lokalapps.lens.internal.domain.usecase.database

import com.lokalapps.lens.internal.data.model.ColumnInfo
import com.lokalapps.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for retrieving the schema of a database table.
 *
 * Returns column information including:
 * - Column name and type
 * - NOT NULL constraint
 * - Default value
 * - Primary key status
 */
class GetTableSchemaUseCase(private val repository: DatabaseRepository) {
  /**
   * Gets the schema for a table.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @return List of [ColumnInfo] in column order
   */
  suspend operator fun invoke(dbName: String, tableName: String): List<ColumnInfo> {
    return repository.getTableSchema(dbName, tableName)
  }
}
