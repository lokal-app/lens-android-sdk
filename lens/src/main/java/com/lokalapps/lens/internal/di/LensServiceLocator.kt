package com.lokalapps.lens.internal.di

import android.content.Context
import com.lokalapps.lens.internal.data.repository.AnalyticsLogRepository
import com.lokalapps.lens.internal.data.repository.AnalyticsLogRepositoryImpl
import com.lokalapps.lens.internal.data.repository.DatabaseRepository
import com.lokalapps.lens.internal.data.repository.DatabaseRepositoryImpl
import com.lokalapps.lens.internal.data.repository.ExceptionLogRepository
import com.lokalapps.lens.internal.data.repository.ExceptionLogRepositoryImpl
import com.lokalapps.lens.internal.data.repository.NetworkLogRepository
import com.lokalapps.lens.internal.data.repository.NetworkLogRepositoryImpl
import com.lokalapps.lens.internal.data.repository.WebSocketLogRepository
import com.lokalapps.lens.internal.data.repository.WebSocketLogRepositoryImpl
import com.lokalapps.lens.internal.data.repository.WebViewLogRepository
import com.lokalapps.lens.internal.data.repository.WebViewLogRepositoryImpl
import com.lokalapps.lens.internal.domain.usecase.analytics.ClearAnalyticsLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.analytics.GetAnalyticsLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.analytics.GetUserPropertiesUseCase
import com.lokalapps.lens.internal.domain.usecase.database.DeleteRowUseCase
import com.lokalapps.lens.internal.domain.usecase.database.ExecuteQueryUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetDatabasesUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetTableSchemaUseCase
import com.lokalapps.lens.internal.domain.usecase.database.GetTablesUseCase
import com.lokalapps.lens.internal.domain.usecase.database.QueryTableUseCase
import com.lokalapps.lens.internal.domain.usecase.database.UpdateCellUseCase
import com.lokalapps.lens.internal.domain.usecase.exceptions.ClearExceptionsUseCase
import com.lokalapps.lens.internal.domain.usecase.exceptions.GetExceptionsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.ClearNetworkLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetNetworkLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetWebSocketLogsUseCase
import com.lokalapps.lens.internal.domain.usecase.network.GetWebViewLogsUseCase

/**
 * Internal service locator for Lens SDK.
 *
 * Replaces Hilt dependency injection with a simple singleton registry. All instances are lazily
 * created and live for the lifetime of the SDK.
 *
 * Must be initialized via [initialize] before any dependencies are accessed. This is called
 * automatically by [LensImpl] during [Lens.install].
 */
internal object LensServiceLocator {

  private var appContext: Context? = null

  /**
   * Initializes the service locator with the application context. Must be called before accessing
   * any dependencies.
   */
  fun initialize(context: Context) {
    appContext = context.applicationContext
  }

  private fun requireContext(): Context {
    return appContext
        ?: throw IllegalStateException(
            "LensServiceLocator not initialized. Call Lens.install() first.")
  }

  // ─── Repositories (singletons) ───────────────────────────────────

  val networkLogRepository: NetworkLogRepository by lazy { NetworkLogRepositoryImpl() }
  val webSocketLogRepository: WebSocketLogRepository by lazy { WebSocketLogRepositoryImpl() }
  val webViewLogRepository: WebViewLogRepository by lazy { WebViewLogRepositoryImpl() }
  val exceptionLogRepository: ExceptionLogRepository by lazy {
    ExceptionLogRepositoryImpl(requireContext())
  }
  val databaseRepository: DatabaseRepository by lazy { DatabaseRepositoryImpl(requireContext()) }
  val analyticsLogRepository: AnalyticsLogRepository by lazy { AnalyticsLogRepositoryImpl() }

  // ─── Use Cases (singletons, each wraps a single repository) ─────

  // Network
  val getNetworkLogsUseCase by lazy { GetNetworkLogsUseCase(networkLogRepository) }
  val getWebSocketLogsUseCase by lazy { GetWebSocketLogsUseCase(webSocketLogRepository) }
  val getWebViewLogsUseCase by lazy { GetWebViewLogsUseCase(webViewLogRepository) }
  val clearNetworkLogsUseCase by lazy {
    ClearNetworkLogsUseCase(networkLogRepository, webSocketLogRepository, webViewLogRepository)
  }

  // Analytics
  val getAnalyticsLogsUseCase by lazy { GetAnalyticsLogsUseCase(analyticsLogRepository) }
  val getUserPropertiesUseCase by lazy { GetUserPropertiesUseCase(analyticsLogRepository) }
  val clearAnalyticsLogsUseCase by lazy { ClearAnalyticsLogsUseCase(analyticsLogRepository) }

  // Exceptions
  val getExceptionsUseCase by lazy { GetExceptionsUseCase(exceptionLogRepository) }
  val clearExceptionsUseCase by lazy { ClearExceptionsUseCase(exceptionLogRepository) }

  // Database
  val getDatabasesUseCase by lazy { GetDatabasesUseCase(databaseRepository) }
  val getTablesUseCase by lazy { GetTablesUseCase(databaseRepository) }
  val getTableSchemaUseCase by lazy { GetTableSchemaUseCase(databaseRepository) }
  val queryTableUseCase by lazy { QueryTableUseCase(databaseRepository) }
  val executeQueryUseCase by lazy { ExecuteQueryUseCase(databaseRepository) }
  val updateCellUseCase by lazy { UpdateCellUseCase(databaseRepository) }
  val deleteRowUseCase by lazy { DeleteRowUseCase(databaseRepository) }
}
