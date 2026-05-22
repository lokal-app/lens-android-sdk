package com.lokalapps.lens.internal.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.lokalapps.lens.R
import com.lokalapps.lens.api.ComposableLensPlugin
import com.lokalapps.lens.internal.presentation.database.DatabaseInspectorScreen
import timber.log.Timber

/**
 * Database Inspector plugin for Lens.
 *
 * Provides live SQLite database inspection and editing capabilities:
 * - Browse all databases in the app
 * - View table schemas and row counts
 * - Query table data with pagination
 * - Execute custom SQL queries
 * - Edit cell values (debug builds only)
 *
 * ## Architecture:
 * - Uses [LensServiceLocator] for [DatabaseRepository] access
 * - MVI pattern for UI state management
 * - Safe read-only access by default
 *
 * ## How it works:
 * Uses Android's [Context.databaseList()] and [SQLiteDatabase] APIs to discover and inspect
 * databases. Schema information is retrieved via `PRAGMA table_info()`.
 *
 * ## Security:
 * - Databases are opened read-only by default
 * - Write operations only work in debug builds
 * - Destructive queries without WHERE are blocked
 *
 * ## Similar to Android Studio:
 * This provides similar functionality to Android Studio's App Inspection Database Inspector, but
 * runs on-device for quick debugging.
 */
class DatabasePlugin : ComposableLensPlugin {

  override val id = "database"
  override val name = "Databases"
  override val icon = R.drawable.ic_lens_database
  override val description = "Inspect and edit SQLite databases"
  override val priority = 80 // After Network (100) and Exceptions (85)

  private var context: Context? = null

  override fun onInitialize(context: Context) {
    this.context = context.applicationContext
    Timber.d("DatabasePlugin: Initialized")
  }

  override fun onEnabled() {
    Timber.d("DatabasePlugin: Enabled")
  }

  override fun onDisabled() {
    Timber.d("DatabasePlugin: Disabled")
  }

  @Composable
  override fun Content() {
    DatabaseInspectorScreen()
  }
}
