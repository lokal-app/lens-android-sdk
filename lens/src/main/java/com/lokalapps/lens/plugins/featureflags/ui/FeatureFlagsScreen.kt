package com.lokalapps.lens.plugins.featureflags.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.api.FeatureFlag
import com.lokalapps.lens.api.FeatureFlagProvider
import com.lokalapps.lens.api.FlagType

/**
 * Feature flags editor screen.
 *
 * Shows all feature flags and allows editing their values.
 */
@Composable
fun FeatureFlagsScreen(provider: FeatureFlagProvider) {
  var flags by remember { mutableStateOf(provider.getFlags()) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Feature Flags",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface)

          IconButton(
              onClick = {
                provider.resetAll()
                flags = provider.getFlags()
              }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset all",
                    tint = MaterialTheme.colorScheme.primary)
              }
        }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Changes take effect immediately",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)

    Spacer(modifier = Modifier.height(16.dp))

    if (flags.isEmpty()) {
      Text(
          text = "No feature flags configured",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      LazyColumn {
        items(flags) { flag ->
          FeatureFlagCard(
              flag = flag,
              onValueChanged = { newValue ->
                provider.setFlag(flag.key, newValue)
                flags = provider.getFlags()
              })
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
private fun FeatureFlagCard(flag: FeatureFlag, onValueChanged: (Any) -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = flag.name,
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface)

                  Text(
                      text = flag.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)

                  Text(
                      text = "Key: ${flag.key}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.outline)
                }

                when (flag.type) {
                  FlagType.BOOLEAN -> {
                    Switch(
                        checked = flag.currentValue as? Boolean ?: false,
                        onCheckedChange = { onValueChanged(it) })
                  }
                  else -> {
                    // For non-boolean flags, show a text field
                    var textValue by remember { mutableStateOf(flag.currentValue.toString()) }

                    TextField(
                        value = textValue,
                        onValueChange = { newText ->
                          textValue = newText
                          val parsed =
                              when (flag.type) {
                                FlagType.STRING -> newText
                                FlagType.INT -> newText.toIntOrNull() ?: flag.defaultValue
                                FlagType.LONG -> newText.toLongOrNull() ?: flag.defaultValue
                                FlagType.DOUBLE -> newText.toDoubleOrNull() ?: flag.defaultValue
                                else -> newText
                              }
                          onValueChanged(parsed)
                        },
                        modifier = Modifier.fillMaxWidth(0.4f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall)
                  }
                }
              }

          if (flag.currentValue != flag.defaultValue) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Modified (default: ${flag.defaultValue})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
          }
        }
      }
}
