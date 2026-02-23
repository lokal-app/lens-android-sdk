package com.behtar.lens.internal.presentation.database

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.behtar.lens.internal.presentation.LensViewModelFactory
import com.behtar.lens.internal.presentation.database.components.DatabaseListView
import com.behtar.lens.internal.presentation.database.components.EmptyDataMessage
import com.behtar.lens.internal.presentation.database.components.QueryEditorView
import com.behtar.lens.internal.presentation.database.components.SchemaView
import com.behtar.lens.internal.presentation.database.components.TableDataView
import com.behtar.lens.internal.presentation.database.components.TableListView

/**
 * Main screen for the Database Inspector plugin.
 *
 * Provides navigation between:
 * - Database list
 * - Table list (per database)
 * - Table data view (with pagination)
 * - Table schema view
 * - SQL query editor
 *
 * Uses MVI pattern with [DatabaseViewModel] for state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseInspectorScreen(
    viewModel: DatabaseViewModel = viewModel(factory = LensViewModelFactory)
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // Show error in snackbar
  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.onEvent(DatabaseEvent.ClearError)
    }
  }

  Scaffold(
      topBar = {
        DatabaseTopBar(
            currentScreen = uiState.currentScreen,
            selectedDatabase = uiState.selectedDatabase?.name,
            selectedTable = uiState.selectedTable?.name,
            onNavigateBack = { viewModel.onEvent(DatabaseEvent.NavigateBack) },
            onRefresh = { viewModel.onEvent(DatabaseEvent.Refresh) })
      },
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(
              snackbarData = data,
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer)
        }
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Main content based on current screen
          when (val screen = uiState.currentScreen) {
            is DatabaseScreen.DatabaseList -> {
              DatabaseListView(
                  databases = uiState.databases,
                  onDatabaseClick = { viewModel.onEvent(DatabaseEvent.SelectDatabase(it)) },
                  modifier = Modifier.fillMaxSize())
            }
            is DatabaseScreen.TableList -> {
              TableListView(
                  tables = uiState.tables,
                  includeSystemTables = uiState.includeSystemTables,
                  onTableClick = { viewModel.onEvent(DatabaseEvent.SelectTable(it)) },
                  onSchemaClick = { table ->
                    viewModel.onEvent(DatabaseEvent.SelectTable(table))
                    viewModel.onEvent(DatabaseEvent.ViewSchema)
                  },
                  onToggleSystemTables = { viewModel.onEvent(DatabaseEvent.ToggleSystemTables) },
                  onQueryClick = { viewModel.onEvent(DatabaseEvent.OpenQueryEditor) },
                  modifier = Modifier.fillMaxSize())
            }
            is DatabaseScreen.TableData -> {
              if (uiState.queryResult.rowCount == 0 && !uiState.isLoading) {
                EmptyDataMessage(modifier = Modifier.fillMaxSize())
              } else {
                TableDataView(
                    result = uiState.queryResult,
                    currentPage = uiState.currentPage,
                    pageSize = uiState.pageSize,
                    sortColumn = uiState.sortColumn,
                    sortAscending = uiState.sortAscending,
                    onSortClick = { viewModel.onEvent(DatabaseEvent.SortBy(it)) },
                    onPageChange = { viewModel.onEvent(DatabaseEvent.LoadPage(it)) },
                    modifier = Modifier.fillMaxSize())
              }
            }
            is DatabaseScreen.TableSchema -> {
              SchemaView(
                  columns = uiState.tableSchema,
                  tableName = uiState.selectedTable?.name ?: "",
                  modifier = Modifier.fillMaxSize())
            }
            is DatabaseScreen.QueryEditor -> {
              QueryEditorView(
                  dbName = screen.dbName,
                  query = uiState.customQuery,
                  result =
                      uiState.queryResult.takeIf { it.columns.isNotEmpty() || it.error != null },
                  isLoading = uiState.isLoading,
                  onQueryChange = { viewModel.onEvent(DatabaseEvent.UpdateQuery(it)) },
                  onExecute = { viewModel.onEvent(DatabaseEvent.ExecuteQuery) },
                  modifier = Modifier.fillMaxSize())
            }
          }

          // Loading overlay
          if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabaseTopBar(
    currentScreen: DatabaseScreen,
    selectedDatabase: String?,
    selectedTable: String?,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
  val (title, subtitle) =
      when (currentScreen) {
        is DatabaseScreen.DatabaseList -> "Databases" to null
        is DatabaseScreen.TableList -> selectedDatabase to "Tables"
        is DatabaseScreen.TableData -> selectedTable to "Data"
        is DatabaseScreen.TableSchema -> selectedTable to "Schema"
        is DatabaseScreen.QueryEditor -> selectedDatabase to "Query Editor"
      }

  TopAppBar(
      title = {
        Column {
          Text(text = title ?: "Database Inspector", maxLines = 1, overflow = TextOverflow.Ellipsis)
          subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      },
      navigationIcon = {
        if (currentScreen !is DatabaseScreen.DatabaseList) {
          IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        } else {
          Row(
              modifier = Modifier.padding(start = 8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
              }
        }
      },
      actions = {
        if (currentScreen !is DatabaseScreen.QueryEditor) {
          IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
          }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
}
