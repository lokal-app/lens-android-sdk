package com.behtar.lens.internal.presentation.exceptions

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.behtar.lens.internal.data.model.ExceptionLogEntry
import com.behtar.lens.internal.presentation.LensViewModelFactory

/**
 * Exceptions/Crashes viewer screen. Shows uncaught exceptions and handled exceptions captured by
 * Lens.
 */
@Composable
fun ExceptionsScreen(
    modifier: Modifier = Modifier,
    viewModel: ExceptionsViewModel = viewModel(factory = LensViewModelFactory)
) {
  val uiState by viewModel.uiState.collectAsState()
  val exceptions by viewModel.exceptions.collectAsState()

  if (uiState.selectedEntry != null) {
    ExceptionDetailScreen(
        entry = uiState.selectedEntry!!,
        onBack = { viewModel.onEvent(ExceptionsEvent.NavigateBack) })
  } else {
    ExceptionListScreen(
        exceptions = exceptions,
        onEntryClick = { viewModel.onEvent(ExceptionsEvent.SelectEntry(it)) },
        onClear = { viewModel.onEvent(ExceptionsEvent.ClearExceptions) })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExceptionListScreen(
    exceptions: List<ExceptionLogEntry>,
    onEntryClick: (ExceptionLogEntry) -> Unit,
    onClear: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
        title = { Text("Exceptions (${exceptions.size})") },
        actions = {
          if (exceptions.isNotEmpty()) {
            IconButton(onClick = onClear) {
              Icon(Icons.Default.Delete, contentDescription = "Clear")
            }
          }
        })

    if (exceptions.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              text = "No exceptions captured",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "Uncaught exceptions will appear here",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline)
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(exceptions, key = { it.id }) { entry ->
          ExceptionCard(entry = entry, onClick = { onEntryClick(entry) })
        }
      }
    }
  }
}

@Composable
private fun ExceptionCard(entry: ExceptionLogEntry, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .clickable(onClick = onClick),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (entry.isHandled) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector =
                          if (entry.isHandled) Icons.Default.BugReport else Icons.Default.Warning,
                      contentDescription = null,
                      modifier = Modifier.size(20.dp),
                      tint =
                          if (entry.isHandled) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.error)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                      text = entry.simpleClassName,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = if (entry.isHandled) "Handled" else "Crash",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (entry.isHandled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.background(
                                if (entry.isHandled) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp))
              }
          Spacer(modifier = Modifier.height(4.dp))
          entry.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
          }
          Text(
              text = entry.topStackLine,
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.outline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = entry.formattedTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "Thread: ${entry.threadName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
              }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExceptionDetailScreen(entry: ExceptionLogEntry, onBack: () -> Unit) {
  val clipboardManager = LocalClipboardManager.current

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
        title = { Text("Exception Details") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
              onClick = {
                val fullText = buildString {
                  appendLine("Exception: ${entry.exceptionClass}")
                  appendLine("Message: ${entry.message ?: "N/A"}")
                  appendLine("Thread: ${entry.threadName}")
                  appendLine("Time: ${entry.formattedDate}")
                  appendLine("Status: ${if (entry.isHandled) "Handled" else "Uncaught"}")
                  appendLine()
                  appendLine("Stack Trace:")
                  append(entry.stackTrace)
                }
                clipboardManager.setText(AnnotatedString(fullText))
              }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
              }
        })

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
      SectionTitle("Overview")
      DetailRow("Exception", entry.exceptionClass)
      entry.message?.let { DetailRow("Message", it) }
      DetailRow("Thread", entry.threadName)
      DetailRow("Time", entry.formattedDate)
      DetailRow(
          "Status",
          if (entry.isHandled) "Handled" else "Uncaught Crash",
          valueColor = if (entry.isHandled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)

      if (entry.additionalInfo.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle("Additional Info")
        entry.additionalInfo.forEach { (key, value) -> DetailRow(key, value) }
      }

      Spacer(modifier = Modifier.height(16.dp))
      SectionTitle("Stack Trace")
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                  .horizontalScroll(rememberScrollState())
                  .padding(12.dp)) {
            Text(
                text = entry.stackTrace,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
    }
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.width(100.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = valueColor,
        modifier = Modifier.weight(1f))
  }
}
