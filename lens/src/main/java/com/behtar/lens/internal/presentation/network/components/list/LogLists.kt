package com.behtar.lens.internal.presentation.network.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.data.model.NetworkLogEntry
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.data.model.WebViewLogEntry
import com.behtar.lens.internal.presentation.common.ClearHeader
import com.behtar.lens.internal.presentation.common.EmptyState
import com.behtar.lens.internal.presentation.theme.HttpMethodColors
import com.behtar.lens.internal.presentation.theme.HttpStatusColors
import com.behtar.lens.internal.presentation.theme.WebSocketStatusColors

@Composable
fun ApiLogList(
    logs: List<NetworkLogEntry>,
    onEntryClick: (NetworkLogEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    ClearHeader(onClear = onClear)
    if (logs.isEmpty()) {
      EmptyState(
          icon = Icons.Default.Public,
          title = "No API requests yet",
          subtitle = "App API calls will appear here")
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs, key = { it.id }) { entry ->
          HttpLogCard(entry = entry, onClick = { onEntryClick(entry) })
        }
      }
    }
  }
}

@Composable
fun WebViewLogList(
    logs: List<WebViewLogEntry>,
    onEntryClick: (WebViewLogEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    ClearHeader(onClear = onClear)
    if (logs.isEmpty()) {
      EmptyState(
          icon = Icons.Default.Web,
          title = "No WebView requests yet",
          subtitle = "WebView network calls will appear here")
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs, key = { it.id }) { entry ->
          WebViewLogCard(entry = entry, onClick = { onEntryClick(entry) })
        }
      }
    }
  }
}

@Composable
fun WebSocketLogList(
    connections: List<WebSocketLogEntry>,
    onEntryClick: (WebSocketLogEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    ClearHeader(onClear = onClear)
    if (connections.isEmpty()) {
      EmptyState(
          icon = Icons.Default.Sync,
          title = "No WebSocket connections yet",
          subtitle = "WebSocket connections will appear here")
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(connections, key = { it.id }) { entry ->
          WebSocketLogCard(entry = entry, onClick = { onEntryClick(entry) })
        }
      }
    }
  }
}

@Composable
private fun HttpLogCard(
    entry: NetworkLogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val statusColor = entry.statusColor
  val methodColor = HttpMethodColors.forMethod(entry.method)

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              StatusDot(color = statusColor)
              Spacer(modifier = Modifier.width(12.dp))
              MethodBadge(method = entry.method, color = methodColor)
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.path,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = entry.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.End) {
                if (entry.isInProgress) {
                  Text(
                      text = "...",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline)
                } else {
                  Text(
                      text = "${entry.responseCode}",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = statusColor)
                  Text(
                      text = entry.durationString,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline)
                }
              }
            }
      }
}

@Composable
private fun WebViewLogCard(
    entry: WebViewLogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val statusColor = entry.statusColor
  val methodColor = HttpMethodColors.forMethod(entry.method)

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              StatusDot(color = statusColor)
              Spacer(modifier = Modifier.width(12.dp))
              MethodBadge(method = entry.method, color = methodColor)
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.path,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Row {
                  Text(
                      text = entry.host,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline,
                      maxLines = 1)
                  if (entry.isMainFrame) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• Main Frame",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.End) {
                when (entry.statusType) {
                  WebViewLogEntry.StatusType.PENDING ->
                      Text(
                          text = "...",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.outline)
                  WebViewLogEntry.StatusType.SUCCESS ->
                      Text(
                          text = "${entry.statusCode ?: "OK"}",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = HttpStatusColors.success)
                  WebViewLogEntry.StatusType.ERROR ->
                      Text(
                          text = "Error",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = HttpStatusColors.serverError)
                  else ->
                      Text(
                          text = "${entry.statusCode ?: "?"}",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = statusColor)
                }
                // Show duration if available, otherwise fall back to MIME type
                val durationText = entry.durationString
                if (durationText != null) {
                  Text(
                      text = durationText,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline)
                } else {
                  entry.mimeType?.let {
                    Text(
                        text = it.substringBefore(";").take(20),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                  }
                }
              }
            }
      }
}

@Composable
private fun WebSocketLogCard(
    entry: WebSocketLogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val statusColor = entry.statusColor

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              StatusDot(color = statusColor)
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                  text = "WS",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF9C27B0),
                  modifier =
                      Modifier.background(
                              Color(0xFF9C27B0).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.host,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = entry.formattedTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor)
                Text(
                    text = "${entry.messageCount} msgs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
              }
            }
      }
}

@Composable
private fun StatusDot(color: Color, modifier: Modifier = Modifier) {
  Box(modifier = modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun MethodBadge(method: String, color: Color, modifier: Modifier = Modifier) {
  Text(
      text = method,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = color,
      modifier =
          modifier
              .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp))
}

private val NetworkLogEntry.statusColor: Color
  get() =
      when (statusType) {
        NetworkLogEntry.StatusType.SUCCESS -> HttpStatusColors.success
        NetworkLogEntry.StatusType.REDIRECT -> HttpStatusColors.redirect
        NetworkLogEntry.StatusType.CLIENT_ERROR -> HttpStatusColors.clientError
        NetworkLogEntry.StatusType.SERVER_ERROR -> HttpStatusColors.serverError
        NetworkLogEntry.StatusType.ERROR -> HttpStatusColors.serverError
        NetworkLogEntry.StatusType.PENDING -> HttpStatusColors.pending
        NetworkLogEntry.StatusType.UNKNOWN -> HttpStatusColors.unknown
      }

private val WebViewLogEntry.statusColor: Color
  get() =
      when (statusType) {
        WebViewLogEntry.StatusType.SUCCESS -> HttpStatusColors.success
        WebViewLogEntry.StatusType.REDIRECT -> HttpStatusColors.redirect
        WebViewLogEntry.StatusType.CLIENT_ERROR -> HttpStatusColors.clientError
        WebViewLogEntry.StatusType.SERVER_ERROR -> HttpStatusColors.serverError
        WebViewLogEntry.StatusType.ERROR -> HttpStatusColors.serverError
        WebViewLogEntry.StatusType.PENDING -> HttpStatusColors.pending
        WebViewLogEntry.StatusType.UNKNOWN -> HttpStatusColors.unknown
      }

private val WebSocketLogEntry.statusColor: Color
  get() =
      when (status) {
        WebSocketLogEntry.Status.CONNECTING -> WebSocketStatusColors.connecting
        WebSocketLogEntry.Status.OPEN -> WebSocketStatusColors.open
        WebSocketLogEntry.Status.CLOSING -> WebSocketStatusColors.closing
        WebSocketLogEntry.Status.CLOSED -> WebSocketStatusColors.closed
        WebSocketLogEntry.Status.FAILED -> WebSocketStatusColors.failed
      }
