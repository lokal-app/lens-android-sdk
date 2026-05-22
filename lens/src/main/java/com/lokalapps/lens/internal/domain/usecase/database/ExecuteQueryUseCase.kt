package com.lokalapps.lens.internal.domain.usecase.database

import com.lokalapps.lens.internal.data.model.QueryResult
import com.lokalapps.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for executing custom SQL queries.
 *
 * Supports:
 * - SELECT queries (returns result data)
 * - PRAGMA queries (returns result data)
 * - INSERT/UPDATE/DELETE (returns success message, debug only)
 *
 * ## Security
 * - Write operations are blocked in release builds
 * - Destructive queries without WHERE are blocked
 */
class ExecuteQueryUseCase(private val repository: DatabaseRepository) {
  /**
   * Executes a custom SQL query.
   *
   * @param dbName The database filename
   * @param query The SQL query to execute
   * @return [QueryResult] with results or error message
   */
  suspend operator fun invoke(dbName: String, query: String): QueryResult {
    return repository.executeQuery(dbName, query)
  }
}
