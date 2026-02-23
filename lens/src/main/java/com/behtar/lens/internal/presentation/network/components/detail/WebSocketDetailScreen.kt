package com.behtar.lens.internal.presentation.network.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.data.model.WebSocketLogEntry
import com.behtar.lens.internal.presentation.common.DetailRow
import com.behtar.lens.internal.presentation.common.SectionTitle
import com.behtar.lens.internal.presentation.theme.WebSocketStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSocketDetailScreen(
    entry: WebSocketLogEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
        title = {
          Column {
            Text(
                text = entry.host,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = entry.status.name,
                style = MaterialTheme.typography.bodySmall,
                color = entry.statusColor)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        })

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      SectionTitle("Connection Info")
      DetailRow("URL", entry.url)
      DetailRow("Status", entry.status.name)
      DetailRow("Connected At", entry.formattedTimestamp)
      entry.closeCode?.let { DetailRow("Close Code", it.toString()) }
      entry.closeReason?.let { DetailRow("Close Reason", it) }
      entry.errorMessage?.let {
        DetailRow("Error", it, valueColor = MaterialTheme.colorScheme.error)
      }

      Spacer(modifier = Modifier.height(16.dp))

      SectionTitle("Messages (${entry.messageCount})")

      if (entry.messages.isEmpty()) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 8.dp))
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(entry.messages) { message -> MessageCard(message) }
        }
      }
    }
  }
}

@Composable
private fun MessageCard(message: WebSocketLogEntry.Message) {
  val isReceived = message.direction == WebSocketLogEntry.Message.Direction.RECEIVED
  val color = if (isReceived) WebSocketStatusColors.received else WebSocketStatusColors.sent

  Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.background(color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                  Text(
                      text = if (isReceived) "←" else "→",
                      color = Color.White,
                      fontWeight = FontWeight.Bold)
                }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = message.formattedTimestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = message.content,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              maxLines = 10,
              overflow = TextOverflow.Ellipsis)
        }
      }
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
