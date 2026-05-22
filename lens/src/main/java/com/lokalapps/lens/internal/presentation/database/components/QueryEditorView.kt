package com.lokalapps.lens.internal.presentation.database.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.data.model.QueryResult

/**
 * SQL query editor with result display.
 *
 * Features:
 * - Multi-line SQL input
 * - Execute button
 * - Results in scrollable table
 * - Error display
 *
 * @param dbName Database name for display
 * @param query Current query text
 * @param result Query result (null if not executed)
 * @param isLoading Whether query is executing
 * @param onQueryChange Callback when query text changes
 * @param onExecute Callback to execute query
 * @param modifier Modifier for the component
 */
@Composable
fun QueryEditorView(
    dbName: String,
    query: String,
    result: QueryResult?,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    // Header
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "SQL Query Editor", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Database: $dbName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    // Query input
    Column(modifier = Modifier.padding(16.dp)) {
      OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          modifier = Modifier.fillMaxWidth().height(150.dp),
          label = { Text("SQL Query") },
          placeholder = { Text("SELECT * FROM table_name LIMIT 50") },
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
          enabled = !isLoading)

      Spacer(modifier = Modifier.height(12.dp))

      Button(
          onClick = onExecute,
          modifier = Modifier.align(Alignment.End),
          enabled = query.isNotBlank() && !isLoading) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoading) "Executing..." else "Execute")
          }
    }

    // Results
    if (result != null) {
      Surface(modifier = Modifier.fillMaxWidth().weight(1f), tonalElevation = 1.dp) {
        Column {
          // Result header
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        if (result.isSuccess) {
                          "${result.rowCount} rows"
                        } else {
                          "Error"
                        },
                    style = MaterialTheme.typography.bodyMedium)

                Text(
                    text = result.formattedExecutionTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

          // Error or results
          if (result.error != null) {
            ErrorDisplay(error = result.error)
          } else if (result.columns.isEmpty()) {
            EmptyResultMessage()
          } else {
            QueryResultTable(result = result)
          }
        }
      }
    }
  }
}

@Composable
private fun ErrorDisplay(error: String) {
  Surface(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      color = MaterialTheme.colorScheme.errorContainer,
      shape = MaterialTheme.shapes.medium) {
        Text(
            text = error,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onErrorContainer)
      }
}

@Composable
private fun EmptyResultMessage() {
  Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
    Text(
        text = "No results",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun QueryResultTable(result: QueryResult) {
  val horizontalScrollState = rememberScrollState()

  LazyColumn(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
    // Header
    item {
      Row(
          modifier =
              Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                  .padding(vertical = 2.dp)) {
            result.columns.forEach { column ->
              Box(modifier = Modifier.width(COLUMN_WIDTH).padding(8.dp)) {
                Text(
                    text = column,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }
            }
          }
    }

    // Data rows
    itemsIndexed(result.rows) { index, row ->
      val backgroundColor =
          if (index % 2 == 0) {
            MaterialTheme.colorScheme.surface
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          }

      Row(modifier = Modifier.background(backgroundColor)) {
        result.columns.forEach { column ->
          val value = row[column]
          val displayValue =
              when (value) {
                null -> "NULL"
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
  }
}

private val COLUMN_WIDTH = 150.dp
