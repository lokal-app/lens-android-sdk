package com.behtar.lens.internal.presentation.analytics.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.behtar.lens.internal.analytics.FirebaseAnalyticsValidator
import com.behtar.lens.internal.data.model.UserPropertyEntry

private val FirebaseWarningAmber = Color(0xFFF59E0B)

/**
 * Displays a list of user property updates.
 *
 * Each entry shows:
 * - User ID (or "Anonymous")
 * - Timestamp
 * - Destination
 * - Property count and values
 *
 * @param properties List of user property entries
 * @param modifier Modifier for the list
 */
@Composable
fun UserPropertiesView(properties: List<UserPropertyEntry>, modifier: Modifier = Modifier) {
  if (properties.isEmpty()) {
    EmptyPropertiesMessage(modifier)
  } else {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item { Spacer(modifier = Modifier.height(8.dp)) }

          items(properties, key = { it.id }) { property ->
            val validation =
                remember(property.id) { FirebaseAnalyticsValidator.validateUserProperty(property) }
            UserPropertyCard(property = property, hasFirebaseViolations = validation.hasViolations)
          }

          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
  }
}

@Composable
private fun UserPropertyCard(property: UserPropertyEntry, hasFirebaseViolations: Boolean) {
  var isExpanded by remember { mutableStateOf(false) }
  val hasMoreProperties = property.properties.size > 5
  val context = LocalContext.current
  val validation =
      remember(property.id) { FirebaseAnalyticsValidator.validateUserProperty(property) }

  Card(
      modifier = Modifier.fillMaxWidth().animateContentSize(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
      if (hasFirebaseViolations) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(FirebaseWarningAmber))
      }
      Column(modifier = Modifier.padding(12.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)) {
                    Text(
                        text = property.userId ?: "Anonymous",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color =
                            if (property.userId != null) {
                              MaterialTheme.colorScheme.primary
                            } else {
                              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            })

                    // Only show destination for single-SDK events
                    property.displayDestination?.let { destination ->
                      Surface(
                          color = MaterialTheme.colorScheme.secondaryContainer,
                          shape = MaterialTheme.shapes.extraSmall) {
                            Text(
                                text = destination,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall)
                          }
                    }
                  }

              Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = property.formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                    // Copy button
                    IconButton(
                        onClick = {
                          val text = buildString {
                            appendLine("User ID: ${property.userId ?: "Anonymous"}")
                            appendLine("Time: ${property.formattedTime}")
                            appendLine("Destinations: ${property.destinations.joinToString(", ")}")
                            appendLine("Properties:")
                            property.properties.forEach { (key, value) ->
                              appendLine("  $key: ${value?.toString() ?: "null"}")
                            }
                          }
                          copyToClipboard(context, text, "User properties")
                        },
                        modifier = Modifier.size(32.dp)) {
                          Icon(
                              imageVector = Icons.Default.ContentCopy,
                              contentDescription = "Copy properties",
                              modifier = Modifier.size(16.dp),
                              tint = MaterialTheme.colorScheme.primary)
                        }
                  }
            }

        // Properties
        if (property.properties.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))

          val propertiesToShow =
              if (isExpanded || !hasMoreProperties) {
                property.properties.entries.toList()
              } else {
                property.properties.entries.take(5)
              }

          propertiesToShow.forEach { (key, value) ->
            val propViolations = validation.propertyViolations[key] ?: emptyList()
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                    text = "$key:",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (propViolations.isNotEmpty()) FirebaseWarningAmber
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                Text(
                    text = value?.toString() ?: "null",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
              }
              propViolations.forEach { violation ->
                Text(
                    text = "⚠ ${violation.message()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FirebaseWarningAmber,
                )
              }
            }
          }

          // Show more/less toggle
          if (hasMoreProperties) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    if (isExpanded) {
                      "Show less"
                    } else {
                      "... and ${property.properties.size - 5} more (tap to expand)"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(vertical = 4.dp))
          }
        }
      }
    }
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
private fun EmptyPropertiesMessage(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No User Properties",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "User property updates will appear here when set.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}
