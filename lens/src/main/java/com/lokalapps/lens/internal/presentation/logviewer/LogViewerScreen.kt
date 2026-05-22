package com.lokalapps.lens.internal.presentation.logviewer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lokalapps.lens.internal.data.model.LogEntry
import com.lokalapps.lens.internal.data.model.LogLevel
import com.lokalapps.lens.internal.data.repository.LogRepository

/**
 * Log Viewer screen.
 *
 * Displays captured Timber logs with filtering by level and search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(logRepository: LogRepository) {
  val allLogs by logRepository.logs.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedLevels by remember { mutableStateOf(setOf<LogLevel>()) }
  var expandedLogId by remember { mutableStateOf<String?>(null) }

  // Filter logs
  val filteredLogs =
      remember(allLogs, searchQuery, selectedLevels) {
        allLogs.filter { log ->
          // Level filter (empty = all)
          val matchesLevel =
              selectedLevels.isEmpty() || LogLevel.fromPriority(log.priority) in selectedLevels

          // Search filter
          val matchesSearch =
              searchQuery.isBlank() ||
                  log.message.contains(searchQuery, ignoreCase = true) ||
                  log.tag?.contains(searchQuery, ignoreCase = true) == true

          matchesLevel && matchesSearch
        }
      }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Log Viewer") },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface),
            actions = {
              IconButton(onClick = { logRepository.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear logs")
              }
            })
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Search bar
          OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              placeholder = { Text("Search logs...") },
              leadingIcon = { Icon(Icons.Default.Search, null) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, "Clear search")
                  }
                }
              },
              singleLine = true)

          // Level filter chips
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogLevel.entries.forEach { level ->
                  val isSelected = level in selectedLevels
                  FilterChip(
                      selected = isSelected,
                      onClick = {
                        selectedLevels =
                            if (isSelected) {
                              selectedLevels - level
                            } else {
                              selectedLevels + level
                            }
                      },
                      label = { Text(level.label) },
                      leadingIcon = {
                        Box(
                            modifier =
                                Modifier.size(12.dp)
                                    .clip(CircleShape)
                                    .background(getLogLevelColor(level.priority)))
                      },
                      colors =
                          FilterChipDefaults.filterChipColors(
                              selectedContainerColor =
                                  getLogLevelColor(level.priority).copy(alpha = 0.2f)))
                }
              }

          // Stats row
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${filteredLogs.size} logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selectedLevels.isNotEmpty()) {
                  Text(
                      text = "Clear filters",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.clickable { selectedLevels = emptySet() })
                }
              }

          // Log list
          if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          if (allLogs.isEmpty()) "No logs captured yet"
                          else "No logs match filters",
                      style = MaterialTheme.typography.bodyLarge,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          } else {
            LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize()) {
              items(filteredLogs, key = { it.id }) { log ->
                LogEntryCard(
                    log = log,
                    isExpanded = log.id == expandedLogId,
                    onClick = { expandedLogId = if (expandedLogId == log.id) null else log.id })
              }
              item { Spacer(modifier = Modifier.height(16.dp)) }
            }
          }
        }
      }
}

@Composable
private fun LogEntryCard(log: LogEntry, isExpanded: Boolean, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp)
              .clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor = getLogLevelColor(log.priority).copy(alpha = 0.05f)),
      shape = RoundedCornerShape(4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
          // Header row: time, level badge, tag
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Timestamp
            Text(
                text = log.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.width(8.dp))

            // Level badge
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(2.dp))
                        .background(getLogLevelColor(log.priority))
                        .padding(horizontal = 4.dp, vertical = 1.dp)) {
                  Text(
                      text = log.priorityChar.toString(),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 10.sp)
                }

            // Tag
            log.tag?.let { tag ->
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = tag,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false))
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Message
          Text(
              text = log.message,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = if (isExpanded) Int.MAX_VALUE else 2,
              overflow = TextOverflow.Ellipsis)

          // Throwable (if present and expanded)
          if (isExpanded && log.throwable != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.throwable.stackTraceToString(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp)
          }
        }
      }
}

/** Returns a color for the given log priority level. */
@Composable
private fun getLogLevelColor(priority: Int): Color {
  return when (priority) {
    Log.VERBOSE -> Color(0xFF9E9E9E) // Gray
    Log.DEBUG -> Color(0xFF2196F3) // Blue
    Log.INFO -> Color(0xFF4CAF50) // Green
    Log.WARN -> Color(0xFFFF9800) // Orange
    Log.ERROR -> Color(0xFFF44336) // Red
    Log.ASSERT -> Color(0xFF9C27B0) // Purple
    else -> Color(0xFF9E9E9E) // Gray
  }
}
