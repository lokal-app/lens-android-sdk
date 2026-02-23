package com.behtar.lens.internal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.behtar.lens.internal.di.LensServiceLocator
import com.behtar.lens.internal.presentation.analytics.AnalyticsViewModel
import com.behtar.lens.internal.presentation.database.DatabaseViewModel
import com.behtar.lens.internal.presentation.exceptions.ExceptionsViewModel
import com.behtar.lens.internal.presentation.network.NetworkViewModel

/**
 * ViewModelProvider.Factory for all Lens ViewModels.
 *
 * Replaces Hilt's `@HiltViewModel` + `hiltViewModel()` pattern with a manual factory that pulls
 * dependencies from [LensServiceLocator].
 *
 * This allows the Lens SDK to work without Hilt, making it usable in any Android project regardless
 * of DI framework.
 *
 * ## Usage in Compose screens:
 * ```kotlin
 * val viewModel: NetworkViewModel = viewModel(factory = LensViewModelFactory)
 * ```
 */
internal object LensViewModelFactory : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return when {
      modelClass.isAssignableFrom(NetworkViewModel::class.java) -> {
        NetworkViewModel(
            getNetworkLogsUseCase = LensServiceLocator.getNetworkLogsUseCase,
            getWebSocketLogsUseCase = LensServiceLocator.getWebSocketLogsUseCase,
            getWebViewLogsUseCase = LensServiceLocator.getWebViewLogsUseCase,
            clearNetworkLogsUseCase = LensServiceLocator.clearNetworkLogsUseCase)
            as T
      }
      modelClass.isAssignableFrom(DatabaseViewModel::class.java) -> {
        DatabaseViewModel(
            getDatabasesUseCase = LensServiceLocator.getDatabasesUseCase,
            getTablesUseCase = LensServiceLocator.getTablesUseCase,
            getTableSchemaUseCase = LensServiceLocator.getTableSchemaUseCase,
            queryTableUseCase = LensServiceLocator.queryTableUseCase,
            executeQueryUseCase = LensServiceLocator.executeQueryUseCase,
            updateCellUseCase = LensServiceLocator.updateCellUseCase,
            deleteRowUseCase = LensServiceLocator.deleteRowUseCase)
            as T
      }
      modelClass.isAssignableFrom(ExceptionsViewModel::class.java) -> {
        ExceptionsViewModel(
            getExceptionsUseCase = LensServiceLocator.getExceptionsUseCase,
            clearExceptionsUseCase = LensServiceLocator.clearExceptionsUseCase)
            as T
      }
      modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
        AnalyticsViewModel(
            getAnalyticsLogsUseCase = LensServiceLocator.getAnalyticsLogsUseCase,
            getUserPropertiesUseCase = LensServiceLocator.getUserPropertiesUseCase,
            clearAnalyticsLogsUseCase = LensServiceLocator.clearAnalyticsLogsUseCase)
            as T
      }
      else ->
          throw IllegalArgumentException(
              "Unknown ViewModel class: ${modelClass.name}. " +
                  "LensViewModelFactory only creates Lens internal ViewModels.")
    }
  }
}
