package com.behtar.lens.internal.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.data.model.AnalyticsLogEntry
import com.behtar.lens.internal.data.model.ExceptionLogEntry
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.di.LensServiceLocator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Global search screen that searches across all log types.
 *
 * Provides a unified search experience for:
 * - Network logs (URL, method, request/response bodies, headers)
 * - Exceptions (class name, message, stack trace)
 * - Analytics events (event name, parameters)
 *
 * Uses debounced search (300ms) to avoid excessive filtering during typing.
 */
@OptIn(FlowPreview::class)
@Composable
internal fun GlobalSearchScreen() {
  var query by remember { mutableStateOf("") }
  var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

  // Debounced search
  LaunchedEffect(Unit) {
    snapshotFlow { query }
        .debounce(300)
        .distinctUntilChanged()
        .filter { it.length >= 2 }
        .collect { searchQuery -> results = performSearch(searchQuery) }
  }

  // Clear results when query is too short
  LaunchedEffect(query) {
    if (query.length < 2) {
      results = emptyList()
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search across all logs...") },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        singleLine = true)

    Spacer(modifier = Modifier.height(8.dp))

    if (query.length < 2) {
      Text(
          text = "Type at least 2 characters to search",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline)
    } else if (results.isEmpty() && query.length >= 2) {
      Text(
          text = "No results found for \"$query\"",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline)
    } else {
      Text(
          text = "${results.size} results",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline)
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(results, key = { it.id }) { result -> SearchResultCard(result) }
    }
  }
}

private suspend fun performSearch(query: String): List<SearchResult> {
  val results = mutableListOf<SearchResult>()
  val lowerQuery = query.lowercase()

  // Search network logs
  val networkLogs = LensServiceLocator.networkLogRepository.logs.value
  networkLogs.forEach { entry ->
    if (matchesNetwork(entry, lowerQuery)) {
      results.add(
          SearchResult(
              id = "net_${entry.id}",
              type = SearchResultType.NETWORK,
              title = "${entry.method} ${entry.path}",
              subtitle =
                  "${if (entry.responseCode == 0) "ERR" else entry.responseCode} · ${entry.durationString} · ${entry.host}",
              matchField = findNetworkMatchField(entry, lowerQuery),
              timestamp = entry.requestTimestamp))
    }
  }

  // Search exceptions
  val exceptions = LensServiceLocator.exceptionLogRepository.exceptions.value
  exceptions.forEach { entry ->
    if (matchesException(entry, lowerQuery)) {
      results.add(
          SearchResult(
              id = "exc_${entry.id}",
              type = SearchResultType.EXCEPTION,
              title = entry.simpleClassName,
              subtitle = entry.message ?: "No message",
              matchField = findExceptionMatchField(entry, lowerQuery),
              timestamp = entry.timestamp))
    }
  }

  // Search analytics events
  val analyticsEvents = LensServiceLocator.analyticsLogRepository.getEvents().first()
  analyticsEvents.forEach { entry ->
    if (matchesAnalytics(entry, lowerQuery)) {
      results.add(
          SearchResult(
              id = "ana_${entry.id}",
              type = SearchResultType.ANALYTICS,
              title = entry.eventName,
              subtitle = entry.summary,
              matchField = findAnalyticsMatchField(entry, lowerQuery),
              timestamp = entry.timestamp))
    }
  }

  // Sort by timestamp (newest first)
  return results.sortedByDescending { it.timestamp }
}

private fun matchesNetwork(entry: NetworkLogEntry, query: String): Boolean {
  return entry.url.lowercase().contains(query) ||
      entry.method.lowercase().contains(query) ||
      entry.requestBody?.lowercase()?.contains(query) == true ||
      entry.responseBody?.lowercase()?.contains(query) == true ||
      entry.errorMessage?.lowercase()?.contains(query) == true ||
      entry.requestHeaders.any { (k, v) ->
        k.lowercase().contains(query) || v.lowercase().contains(query)
      } ||
      entry.responseHeaders.any { (k, v) ->
        k.lowercase().contains(query) || v.lowercase().contains(query)
      }
}

private fun findNetworkMatchField(entry: NetworkLogEntry, query: String): String {
  return when {
    entry.url.lowercase().contains(query) -> "URL: ${entry.url}"
    entry.requestBody?.lowercase()?.contains(query) == true -> "Request body"
    entry.responseBody?.lowercase()?.contains(query) == true -> "Response body"
    entry.errorMessage?.lowercase()?.contains(query) == true -> "Error: ${entry.errorMessage}"
    else -> "Headers"
  }
}

private fun matchesException(entry: ExceptionLogEntry, query: String): Boolean {
  return entry.exceptionClass.lowercase().contains(query) ||
      entry.message?.lowercase()?.contains(query) == true ||
      entry.stackTrace.lowercase().contains(query) ||
      entry.threadName.lowercase().contains(query)
}

private fun findExceptionMatchField(entry: ExceptionLogEntry, query: String): String {
  return when {
    entry.exceptionClass.lowercase().contains(query) -> "Class: ${entry.exceptionClass}"
    entry.message?.lowercase()?.contains(query) == true -> "Message: ${entry.message}"
    entry.stackTrace.lowercase().contains(query) -> "Stack trace"
    else -> "Thread: ${entry.threadName}"
  }
}

private fun matchesAnalytics(entry: AnalyticsLogEntry, query: String): Boolean {
  return entry.eventName.lowercase().contains(query) ||
      entry.params.any { (k, v) ->
        k.lowercase().contains(query) || v?.toString()?.lowercase()?.contains(query) == true
      } ||
      entry.destinations.any { it.lowercase().contains(query) }
}

private fun findAnalyticsMatchField(entry: AnalyticsLogEntry, query: String): String {
  return when {
    entry.eventName.lowercase().contains(query) -> "Event name"
    entry.params.any { (k, _) -> k.lowercase().contains(query) } -> "Param key"
    entry.params.any { (_, v) -> v?.toString()?.lowercase()?.contains(query) == true } ->
        "Param value"
    else -> "Destination"
  }
}

/** Types of search results for visual differentiation. */
internal enum class SearchResultType(val label: String) {
  NETWORK("NET"),
  EXCEPTION("EXC"),
  ANALYTICS("ANA")
}

/** Unified search result from any log type. */
internal data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val matchField: String,
    val timestamp: Long
)

/** Card displaying a single search result. */
@Composable
private fun SearchResultCard(result: SearchResult) {
  val typeColor =
      when (result.type) {
        SearchResultType.NETWORK -> MaterialTheme.colorScheme.primary
        SearchResultType.EXCEPTION -> MaterialTheme.colorScheme.error
        SearchResultType.ANALYTICS -> MaterialTheme.colorScheme.tertiary
      }

  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Type badge
            Card(colors = CardDefaults.cardColors(containerColor = typeColor)) {
              Text(
                  text = result.type.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Subtitle
          Text(
              text = result.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          // Match field
          Text(
              text = "Matched in: ${result.matchField}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }
      }
}
