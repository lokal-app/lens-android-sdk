package com.lokalapps.lens.internal.presentation.database.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.data.model.QueryResult

/**
 * Displays table data in a paginated grid view.
 *
 * Features:
 * - Horizontal scrolling for wide tables
 * - Column header sorting
 * - Pagination controls
 * - Row ID display for edit/delete
 *
 * @param result Query result containing columns and rows
 * @param currentPage Current page index
 * @param pageSize Rows per page
 * @param sortColumn Current sort column
 * @param sortAscending Sort direction
 * @param onSortClick Callback when column header is clicked
 * @param onPageChange Callback when page changes
 * @param onRowClick Callback when a row is clicked (for edit)
 * @param modifier Modifier for the component
 */
@Composable
fun TableDataView(
    result: QueryResult,
    currentPage: Int,
    pageSize: Int,
    sortColumn: String?,
    sortAscending: Boolean,
    onSortClick: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onRowClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    // Info bar
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "${result.totalRows} rows total",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)

          Text(
              text = result.formattedExecutionTime,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }

    // Data grid with horizontal scroll
    val horizontalScrollState = rememberScrollState()

    LazyColumn(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
      // Header row
      item {
        HeaderRow(
            columns = result.columns,
            sortColumn = sortColumn,
            sortAscending = sortAscending,
            onSortClick = onSortClick)
      }

      // Data rows
      itemsIndexed(result.rows) { index, row ->
        DataRow(
            row = row,
            columns = result.columns,
            rowIndex = index,
            onClick = {
              // Get rowid from first column if available
              val rowId = (row["rowid"] as? Long) ?: return@DataRow
              onRowClick?.invoke(rowId)
            })
      }
    }

    // Pagination controls
    if (result.totalRows > pageSize) {
      PaginationControls(
          currentPage = currentPage,
          totalPages = result.totalPages(pageSize),
          hasMore = result.hasMore,
          onPageChange = onPageChange)
    }
  }
}

@Composable
private fun HeaderRow(
    columns: List<String>,
    sortColumn: String?,
    sortAscending: Boolean,
    onSortClick: (String) -> Unit
) {
  Row(
      modifier =
          Modifier.background(MaterialTheme.colorScheme.primaryContainer)
              .padding(vertical = 2.dp)) {
        columns.forEach { column ->
          val isSorted = column == sortColumn

          Box(
              modifier =
                  Modifier.width(COLUMN_WIDTH).clickable { onSortClick(column) }.padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text(
                          text = column,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f, fill = false))

                      if (isSorted) {
                        Icon(
                            imageVector =
                                if (sortAscending) {
                                  Icons.Default.ArrowUpward
                                } else {
                                  Icons.Default.ArrowDownward
                                },
                            contentDescription = if (sortAscending) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                      }
                    }
              }
        }
      }
}

@Composable
private fun DataRow(
    row: Map<String, Any?>,
    columns: List<String>,
    rowIndex: Int,
    onClick: () -> Unit
) {
  val backgroundColor =
      if (rowIndex % 2 == 0) {
        MaterialTheme.colorScheme.surface
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      }

  Row(
      modifier =
          Modifier.background(backgroundColor)
              .clickable(onClick = onClick)
              .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)) {
        columns.forEach { column ->
          val value = row[column]
          val displayValue =
              when (value) {
                null -> "NULL"
                is ByteArray -> "[BLOB]"
                else -> value.toString()
              }

          Box(modifier = Modifier.width(COLUMN_WIDTH).padding(8.dp)) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color =
                    if (value == null) {
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                      MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
          }
        }
      }
}

@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    hasMore: Boolean,
    onPageChange: (Int) -> Unit
) {
  Surface(tonalElevation = 2.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Page")
          }

          Spacer(modifier = Modifier.width(16.dp))

          Text(
              text = "Page ${currentPage + 1} of $totalPages",
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center)

          Spacer(modifier = Modifier.width(16.dp))

          IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = hasMore) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Page")
          }
        }
  }
}

@Composable
fun EmptyDataMessage(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No Data",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This table is empty.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

private val COLUMN_WIDTH = 150.dp
