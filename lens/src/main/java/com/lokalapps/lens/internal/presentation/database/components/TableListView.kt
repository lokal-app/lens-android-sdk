package com.lokalapps.lens.internal.presentation.database.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.data.model.TableInfo

/**
 * Displays a list of tables in a database.
 *
 * Shows each table as a card with:
 * - Table name
 * - Row count
 * - Column count
 * - Action buttons (View Data, View Schema)
 *
 * @param tables List of tables to display
 * @param includeSystemTables Whether system tables are shown
 * @param onTableClick Callback when a table is selected for data view
 * @param onSchemaClick Callback when schema is requested
 * @param onToggleSystemTables Callback to toggle system table visibility
 * @param onQueryClick Callback to open query editor
 * @param modifier Modifier for the component
 */
@Composable
fun TableListView(
    tables: List<TableInfo>,
    includeSystemTables: Boolean,
    onTableClick: (TableInfo) -> Unit,
    onSchemaClick: (TableInfo) -> Unit,
    onToggleSystemTables: () -> Unit,
    onQueryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    // Toolbar
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          FilterChip(
              selected = includeSystemTables,
              onClick = onToggleSystemTables,
              label = { Text("System Tables") })

          Surface(
              onClick = onQueryClick,
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimaryContainer)
                      Text(
                          text = "Query",
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
              }
        }

    if (tables.isEmpty()) {
      EmptyTablesMessage()
    } else {
      LazyColumn(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tables, key = { it.name }) { table ->
              TableCard(
                  table = table,
                  onClick = { onTableClick(table) },
                  onSchemaClick = { onSchemaClick(table) })
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(16.dp)) }
          }
    }
  }
}

@Composable
private fun TableCard(table: TableInfo, onClick: () -> Unit, onSchemaClick: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (table.isSystemTable || table.isRoomMetadataTable) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  })) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(
                          text = table.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)

                      if (table.isRoomMetadataTable) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.extraSmall) {
                              Text(
                                  text = "Room",
                                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                  style = MaterialTheme.typography.labelSmall,
                                  color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                      }
                    }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatRowCount(table.rowCount)} rows · ${table.columnCount} columns",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
              }

              IconButton(onClick = onSchemaClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "View Schema",
                    tint = MaterialTheme.colorScheme.primary)
              }
            }
      }
}

@Composable
private fun EmptyTablesMessage() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No Tables",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This database doesn't have any tables yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

private fun formatRowCount(count: Long): String {
  return when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
  }
}
