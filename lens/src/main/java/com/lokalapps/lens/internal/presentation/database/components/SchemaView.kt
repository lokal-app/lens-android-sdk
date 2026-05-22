package com.lokalapps.lens.internal.presentation.database.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.data.model.ColumnInfo

/**
 * Displays the schema of a database table.
 *
 * Shows each column with:
 * - Column name
 * - Data type
 * - Constraints (PRIMARY KEY, NOT NULL, DEFAULT)
 *
 * @param columns List of column info to display
 * @param tableName Name of the table (for display)
 * @param modifier Modifier for the component
 */
@Composable
fun SchemaView(columns: List<ColumnInfo>, tableName: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    // Header
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Table Schema",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text(
            text = tableName,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary)
      }
    }

    // Column list
    LazyColumn(
        modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(columns, key = { it.position }) { column -> ColumnCard(column = column) }

          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
  }
}

@Composable
private fun ColumnCard(column: ColumnInfo) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Column name and primary key indicator
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      if (column.isPrimaryKey) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Primary Key",
                            tint = MaterialTheme.colorScheme.primary)
                      }

                      Text(
                          text = column.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          fontFamily = FontFamily.Monospace,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                // Type badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small) {
                      Text(
                          text = column.displayType,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          style = MaterialTheme.typography.labelMedium,
                          fontFamily = FontFamily.Monospace,
                          color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
              }

          Spacer(modifier = Modifier.height(8.dp))

          // Constraints
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (column.isPrimaryKey) {
              ConstraintBadge("PRIMARY KEY")
            }
            if (column.isNotNull) {
              ConstraintBadge("NOT NULL")
            }
            if (column.defaultValue != null) {
              ConstraintBadge("DEFAULT: ${column.defaultValue}")
            }
          }

          // Position info
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "Column ${column.position + 1}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
      }
}

@Composable
private fun ConstraintBadge(text: String) {
  Surface(
      color = MaterialTheme.colorScheme.tertiaryContainer,
      shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onTertiaryContainer)
      }
}
