package com.behtar.lens.internal.presentation.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.behtar.lens.internal.presentation.LensViewModelFactory
import com.behtar.lens.internal.presentation.analytics.components.EventDetailView
import com.behtar.lens.internal.presentation.analytics.components.EventListView
import com.behtar.lens.internal.presentation.analytics.components.UserPropertiesView
import kotlinx.coroutines.launch

/**
 * Main screen for the Analytics Inspector plugin.
 *
 * Provides:
 * - Tab navigation between Events and User Properties
 * - Real-time event list
 * - Search functionality
 * - Event detail bottom sheet
 * - Clear logs action
 *
 * Uses MVI pattern with [AnalyticsViewModel] for state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsInspectorScreen(
    viewModel: AnalyticsViewModel = viewModel(factory = LensViewModelFactory)
) {
  val uiState by viewModel.uiState.collectAsState()
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()

  Scaffold(
      topBar = {
        AnalyticsTopBar(
            eventCount = uiState.events.size,
            onClearClick = { viewModel.onEvent(AnalyticsEvent.ClearLogs) })
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Search bar
          OutlinedTextField(
              value = uiState.searchQuery,
              onValueChange = { viewModel.onEvent(AnalyticsEvent.UpdateSearch(it)) },
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              placeholder = { Text("Search events or parameters...") },
              leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.onEvent(AnalyticsEvent.UpdateSearch("")) }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true)

          // Tab row
          TabRow(selectedTabIndex = uiState.currentTab.ordinal) {
            Tab(
                selected = uiState.currentTab == AnalyticsTab.Events,
                onClick = { viewModel.onEvent(AnalyticsEvent.SelectTab(AnalyticsTab.Events)) },
                text = { Text("Events (${uiState.filteredEvents.size})") },
                icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = null) })

            Tab(
                selected = uiState.currentTab == AnalyticsTab.UserProperties,
                onClick = {
                  viewModel.onEvent(AnalyticsEvent.SelectTab(AnalyticsTab.UserProperties))
                },
                text = { Text("User Props (${uiState.filteredUserProperties.size})") },
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) })
          }

          // Content based on selected tab
          Box(modifier = Modifier.weight(1f)) {
            when (uiState.currentTab) {
              AnalyticsTab.Events -> {
                EventListView(
                    events = uiState.filteredEvents,
                    onEventClick = { event ->
                      viewModel.onEvent(AnalyticsEvent.SelectEvent(event))
                      scope.launch { sheetState.show() }
                    },
                    modifier = Modifier.fillMaxSize())
              }
              AnalyticsTab.UserProperties -> {
                UserPropertiesView(
                    properties = uiState.filteredUserProperties, modifier = Modifier.fillMaxSize())
              }
            }
          }
        }
      }

  // Event detail bottom sheet
  if (uiState.selectedEvent != null) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.onEvent(AnalyticsEvent.SelectEvent(null)) },
        sheetState = sheetState) {
          EventDetailView(event = uiState.selectedEvent!!, modifier = Modifier.fillMaxWidth())
        }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsTopBar(eventCount: Int, onClearClick: () -> Unit) {
  TopAppBar(
      title = {
        Column {
          Text(text = "Analytics Inspector")
          Text(
              text = "$eventCount events captured",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      actions = {
        IconButton(onClick = onClearClick) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs")
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
}
