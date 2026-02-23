package com.behtar.lens.internal.domain.usecase.database

import com.behtar.lens.internal.data.model.QueryResult
import com.behtar.lens.internal.data.repository.DatabaseRepository

/**
 * Use case for querying table data with pagination.
 *
 * Retrieves rows from a table with support for:
 * - Pagination (page size and page number)
 * - Sorting by column
 * - Sort direction (ascending/descending)
 */
class QueryTableUseCase(private val repository: DatabaseRepository) {
  /**
   * Queries table data with pagination.
   *
   * @param dbName The database filename
   * @param tableName The table name
   * @param pageSize Number of rows per page
   * @param page Page number (0-based)
   * @param orderBy Column to sort by (null for default order)
   * @param ascending Sort direction
   * @return [QueryResult] with rows and pagination info
   */
  suspend operator fun invoke(
      dbName: String,
      tableName: String,
      pageSize: Int = 50,
      page: Int = 0,
      orderBy: String? = null,
      ascending: Boolean = true
  ): QueryResult {
    return repository.queryTable(
        dbName = dbName,
        tableName = tableName,
        pageSize = pageSize,
        page = page,
        orderBy = orderBy,
        ascending = ascending)
  }
}
