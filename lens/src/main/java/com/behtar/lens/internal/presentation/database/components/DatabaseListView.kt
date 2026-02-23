package com.behtar.lens.internal.presentation.database.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.data.model.DatabaseInfo

/**
 * Displays a list of databases with metadata.
 *
 * Shows each database as a card with:
 * - Database name
 * - File size
 * - Table count
 * - Room badge (if applicable)
 *
 * @param databases List of databases to display
 * @param onDatabaseClick Callback when a database is selected
 * @param modifier Modifier for the component
 */
@Composable
fun DatabaseListView(
    databases: List<DatabaseInfo>,
    onDatabaseClick: (DatabaseInfo) -> Unit,
    modifier: Modifier = Modifier
) {
  if (databases.isEmpty()) {
    EmptyDatabasesMessage(modifier)
  } else {
    LazyColumn(
        modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(databases, key = { it.name }) { database ->
            DatabaseCard(database = database, onClick = { onDatabaseClick(database) })
          }
        }
  }
}

@Composable
private fun DatabaseCard(database: DatabaseInfo, onClick: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = database.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (database.isRoomDatabase) {
                  Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "Room",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                      }
                }
              }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${database.tableCount} tables",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))

                Text(
                    text = database.formattedSize,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
              }
        }
      }
}

@Composable
private fun EmptyDatabasesMessage(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No Databases Found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app doesn't have any SQLite databases yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}
