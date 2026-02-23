package com.behtar.lens.internal.domain.usecase.database

import com.behtar.lens.internal.data.model.TableInfo
import com.behtar.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for retrieving all tables in a database.
 *
 * Returns a list of tables with:
 * - Table name
 * - Row count
 * - Column count
 */
class GetTablesUseCase(private val repository: DatabaseRepository) {
  /**
   * Gets all tables in the specified database.
   *
   * @param dbName The database filename
   * @param includeSystemTables Whether to include sqlite_* tables
   * @return List of [TableInfo] sorted by name
   */
  suspend operator fun invoke(
      dbName: String,
      includeSystemTables: Boolean = false
  ): List<TableInfo> {
    return repository.getTables(dbName, includeSystemTables)
  }
}
