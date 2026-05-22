package com.lokalapps.lens.plugins.quickactions.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lokalapps.lens.api.QuickAction
import com.lokalapps.lens.api.QuickActionsProvider

/**
 * Quick actions screen.
 *
 * Shows a list of tappable action shortcuts for debugging.
 */
@Composable
fun QuickActionsScreen(provider: QuickActionsProvider) {
  val actions = remember { provider.getActions() }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface)

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Tap to execute debugging shortcuts",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)

    Spacer(modifier = Modifier.height(16.dp))

    if (actions.isEmpty()) {
      Text(
          text = "No quick actions configured",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      LazyColumn {
        items(actions) { action ->
          QuickActionCard(action)
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
private fun QuickActionCard(action: QuickAction) {
  Card(
      modifier = Modifier.fillMaxWidth().clickable { action.action() },
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (action.isDestructive) {
                    MaterialTheme.colorScheme.errorContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  })) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painter = painterResource(id = action.icon),
                  contentDescription = null,
                  modifier = Modifier.size(32.dp),
                  tint =
                      if (action.isDestructive) {
                        MaterialTheme.colorScheme.error
                      } else {
                        MaterialTheme.colorScheme.primary
                      })

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (action.isDestructive) {
                          MaterialTheme.colorScheme.onErrorContainer
                        } else {
                          MaterialTheme.colorScheme.onSurface
                        })

                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (action.isDestructive) {
                          MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        })
              }
            }
      }
}
