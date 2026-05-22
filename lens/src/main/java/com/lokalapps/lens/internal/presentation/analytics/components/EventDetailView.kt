package com.lokalapps.lens.internal.presentation.analytics.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.internal.analytics.EventValidationResult
import com.lokalapps.lens.internal.analytics.FirebaseAnalyticsValidator
import com.lokalapps.lens.internal.analytics.Violation
import com.lokalapps.lens.internal.data.model.AnalyticsLogEntry

private val FirebaseWarningAmber = Color(0xFFF59E0B)
private val FirebaseWarningAmberContainer = Color(0xFFFEF3C7)

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
  val validation = remember(event.id) { FirebaseAnalyticsValidator.validateEvent(event) }

  LazyColumn(
      modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Firebase violations banner — shown at top when any limit is crossed
        if (validation.hasViolations) {
          item { FirebaseViolationsBanner(validation) }
        }

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
                violations = validation.paramViolations[key] ?: emptyList(),
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
private fun ParameterRow(
    key: String,
    value: Any?,
    violations: List<Violation>,
    onCopy: () -> Unit,
) {
  val hasViolations = violations.isNotEmpty()
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .then(
                  if (hasViolations)
                      Modifier.border(1.dp, FirebaseWarningAmber, MaterialTheme.shapes.small)
                  else Modifier),
      color =
          if (hasViolations) FirebaseWarningAmberContainer
          else MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.small,
  ) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = key,
              style = MaterialTheme.typography.labelMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color =
                  if (hasViolations) FirebaseWarningAmber else MaterialTheme.colorScheme.primary,
          )
          if (hasViolations) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = FirebaseWarningAmber,
                modifier = Modifier.size(12.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatValue(value),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hasViolations) {
          Spacer(modifier = Modifier.height(6.dp))
          violations.forEach { violation ->
            Text(
                text = "⚠ ${violation.message()}",
                style = MaterialTheme.typography.labelSmall,
                color = FirebaseWarningAmber,
            )
          }
        }
      }

      IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy $key",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
      }
    }
  }
}

@Composable
private fun FirebaseViolationsBanner(validation: EventValidationResult) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(FirebaseWarningAmberContainer, RoundedCornerShape(8.dp))
              .border(1.dp, FirebaseWarningAmber, RoundedCornerShape(8.dp))
              .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = FirebaseWarningAmber,
          modifier = Modifier.size(16.dp),
      )
      Text(
          text = "Firebase limit violations — data may be dropped or truncated",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = FirebaseWarningAmber,
      )
    }

    validation.eventNameViolations.forEach { violation ->
      Text(
          text = "• Event name: ${violation.message()}",
          style = MaterialTheme.typography.labelSmall,
          color = FirebaseWarningAmber,
      )
    }

    validation.tooManyParamsViolation.forEach { violation ->
      Text(
          text = "• ${violation.message()}",
          style = MaterialTheme.typography.labelSmall,
          color = FirebaseWarningAmber,
      )
    }

    validation.paramViolations.forEach { (key, violations) ->
      violations.forEach { violation ->
        Text(
            text = "• Param \"$key\": ${violation.message()}",
            style = MaterialTheme.typography.labelSmall,
            color = FirebaseWarningAmber,
        )
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
