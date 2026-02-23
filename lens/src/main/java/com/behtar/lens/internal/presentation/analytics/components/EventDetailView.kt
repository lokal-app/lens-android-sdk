package com.behtar.lens.internal.presentation.analytics.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.data.model.AnalyticsLogEntry

/**
 * Detail view for an analytics event.
 *
 * Shows all event information including:
 * - Event name
 * - Timestamp
 * - Destination
 * - All parameters with values
 * - Revenue info (if applicable)
 *
 * @param event The event to display
 * @param modifier Modifier for the view
 */
@Composable
fun EventDetailView(event: AnalyticsLogEntry, modifier: Modifier = Modifier) {
  val context = LocalContext.current

  LazyColumn(
      modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        item {
          Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = event.eventName,
                      style = MaterialTheme.typography.headlineSmall,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      modifier = Modifier.weight(1f))

                  // Copy event name button
                  IconButton(
                      onClick = { copyToClipboard(context, event.eventName, "Event name") }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy event name",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                      }
                }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = event.formattedTime,
                      style = MaterialTheme.typography.bodyMedium,
                      fontFamily = FontFamily.Monospace,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)

                  // Only show destination for single-SDK events
                  event.displayDestination?.let { destination ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small) {
                          Text(
                              text = destination,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                              style = MaterialTheme.typography.labelMedium)
                        }
                  }
                }

            if (event.isRevenueEvent && event.revenueAmount != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                  color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = "Revenue: ₹${event.revenueAmount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary)
                  }
            }

            // Copy All button
            if (event.params.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedButton(
                  onClick = {
                    val allParams = buildString {
                      appendLine("Event: ${event.eventName}")
                      appendLine("Time: ${event.formattedTime}")
                      appendLine("Destinations: ${event.destinations.joinToString(", ")}")
                      if (event.isRevenueEvent) {
                        appendLine("Revenue: ₹${event.revenueAmount}")
                      }
                      appendLine("Parameters:")
                      event.params.forEach { (key, value) ->
                        appendLine("  $key: ${formatValue(value)}")
                      }
                    }
                    copyToClipboard(context, allParams, "Event details")
                  },
                  modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Copy All")
                  }
            }
          }
        }

        // Divider
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // Parameters header
        item {
          Text(
              text = "Parameters (${event.paramCount})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold)
        }

        // Parameter list
        if (event.params.isEmpty()) {
          item {
            Text(
                text = "No parameters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
          }
        } else {
          items(event.params.entries.toList()) { (key, value) ->
            ParameterRow(
                key = key,
                value = value,
                onCopy = { copyToClipboard(context, "$key: ${formatValue(value)}", key) })
          }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
}

/** Copies text to clipboard and shows a toast. */
private fun copyToClipboard(context: Context, text: String, label: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, "Copied: $label", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ParameterRow(key: String, value: Any?, onCopy: () -> Unit) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.small) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $key",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
          }
        }
      }
}

private fun formatValue(value: Any?): String {
  return when (value) {
    null -> "null"
    is String -> "\"$value\""
    is Number -> value.toString()
    is Boolean -> value.toString()
    is List<*> -> value.joinToString(", ", "[", "]")
    is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "${it.key}: ${it.value}" }
    else -> value.toString()
  }
}
