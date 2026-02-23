package com.behtar.lens.internal.data.model

/**
 * Wrapper for the result of a database query, supporting pagination.
 *
 * Holds both the data and metadata needed for:
 * - Displaying results in a table view
 * - Showing query execution information
 * - Navigating between pages of large result sets
 *
 * @property columns List of column names in result order
 * @property rows List of rows, each as a map of column name to value
 * @property rowCount Number of rows in this page of results
 * @property totalRows Total rows matching the query (for pagination)
 * @property currentPage Current page index (0-based)
 * @property hasMore Whether more pages are available
 * @property executionTimeMs Query execution time in milliseconds
 * @property error Error message if query failed (null on success)
 */
data class QueryResult(
    val columns: List<String>,
    val rows: List<Map<String, Any?>>,
    val rowCount: Int,
    val totalRows: Long,
    val currentPage: Int,
    val hasMore: Boolean,
    val executionTimeMs: Long,
    val error: String? = null
) {
  companion object {
    /** Creates an empty result (for initial state). */
    fun empty() =
        QueryResult(
            columns = emptyList(),
            rows = emptyList(),
            rowCount = 0,
            totalRows = 0,
            currentPage = 0,
            hasMore = false,
            executionTimeMs = 0)

    /** Creates an error result. */
    fun error(message: String) =
        QueryResult(
            columns = emptyList(),
            rows = emptyList(),
            rowCount = 0,
            totalRows = 0,
            currentPage = 0,
            hasMore = false,
            executionTimeMs = 0,
            error = message)
  }

  /** Whether the query executed successfully. */
  val isSuccess: Boolean
    get() = error == null

  /** Returns the total number of pages given the current page size. */
  fun totalPages(pageSize: Int): Int {
    return if (totalRows == 0L) 0 else ((totalRows - 1) / pageSize + 1).toInt()
  }

  /** Formatted execution time string. */
  val formattedExecutionTime: String
    get() =
        when {
          executionTimeMs < 1000 -> "${executionTimeMs}ms"
          else -> String.format("%.2fs", executionTimeMs / 1000.0)
        }
}
