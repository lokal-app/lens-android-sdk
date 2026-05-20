package com.behtar.lens.internal.presentation.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.analytics.FirebaseAnalyticsValidator
import com.behtar.lens.internal.data.model.AnalyticsLogEntry

private val FirebaseWarningAmber = Color(0xFFF59E0B)

/**
 * Displays a list of analytics events.
 *
 * Each event shows:
 * - Event name
 * - Timestamp
 * - Destination badge
 * - Parameter count
 * - Revenue amount (if applicable)
 *
 * @param events List of events to display
 * @param onEventClick Callback when an event is clicked
 * @param modifier Modifier for the list
 */
@Composable
fun EventListView(
    events: List<AnalyticsLogEntry>,
    onEventClick: (AnalyticsLogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
  if (events.isEmpty()) {
    EmptyEventsMessage(modifier)
  } else {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item { Spacer(modifier = Modifier.height(8.dp)) }

          items(events, key = { it.id }) { event ->
            val validation = remember(event.id) {
              FirebaseAnalyticsValidator.validateEvent(event)
            }
            EventCard(
              event = event,
              hasFirebaseViolations = validation.hasViolations,
              onClick = { onEventClick(event) },
            )
          }

          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
  }
}

@Composable
private fun EventCard(
    event: AnalyticsLogEntry,
    hasFirebaseViolations: Boolean,
    onClick: () -> Unit,
) {
  val cardColor = when {
    event.isRevenueEvent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = cardColor),
  ) {
    // IntrinsicSize.Min forces the Row to measure its height from the tallest child,
    // which lets the stripe Box use fillMaxHeight() reliably inside the card.
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
      if (hasFirebaseViolations) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(FirebaseWarningAmber)
        )
      }
      Column(modifier = Modifier.padding(12.dp)) {
        // Top row: Event name, warning icon (if violations), destination badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = event.eventName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (hasFirebaseViolations) {
              Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Firebase limit violation",
                  tint = FirebaseWarningAmber,
                  modifier = Modifier.size(14.dp),
              )
            }
          }

          // Only show destination badge for single-SDK events (not broadcasts)
          event.displayDestination?.let { destination ->
            DestinationBadge(destination = destination)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom row: Timestamp, params, revenue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = event.formattedTime,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )

          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            if (event.paramCount > 0) {
              Text(
                  text = "${event.paramCount} params",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              )
            }

            if (event.isRevenueEvent && event.revenueAmount != null) {
              Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = MaterialTheme.shapes.extraSmall,
              ) {
                Text(
                    text = "₹${event.revenueAmount}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DestinationBadge(destination: String) {
  val (bgColor, textColor) =
      when (destination) {
        "FIREBASE" ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
        "MOENGAGE" ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        "ADJUST" ->
            MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        "CLARITY" ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "FACEBOOK" ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
        else ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
      }

  Surface(color = bgColor, shape = MaterialTheme.shapes.extraSmall) {
    Text(
        text = destination,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = textColor)
  }
}

@Composable
private fun EmptyEventsMessage(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No Events Yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Analytics events will appear here as they are logged.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}
