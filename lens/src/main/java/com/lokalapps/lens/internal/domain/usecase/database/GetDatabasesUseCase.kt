package com.lokalapps.lens.internal.domain.usecase.database

import com.lokalapps.lens.internal.data.model.DatabaseInfo
import com.lokalapps.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for retrieving all SQLite databases in the application.
 *
 * Returns a sorted list of databases with metadata including:
 * - Database name and path
 * - File size
 * - Table count
 * - Whether it's a Room database
 */
class GetDatabasesUseCase(private val repository: DatabaseRepository) {
  /**
   * Gets all databases in the application.
   *
   * @return List of [DatabaseInfo] sorted by name
   */
  suspend operator fun invoke(): List<DatabaseInfo> {
    return repository.getDatabases()
  }
}
