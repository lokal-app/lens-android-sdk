package com.lokalapps.lens.internal.data.model

/**
 * Represents metadata about a SQLite database file in the application.
 *
 * This model is used to display database information in the Database Inspector and for navigation
 * to table/query views.
 *
 * @property name The database filename (e.g., "app_database.db")
 * @property path The absolute file path to the database
 * @property sizeBytes The file size in bytes
 * @property tableCount Number of user tables in the database
 * @property isRoomDatabase Whether this appears to be a Room-managed database (detected by presence
 *   of room_master_table)
 */
data class DatabaseInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val tableCount: Int,
    val isRoomDatabase: Boolean = false
) {
  /** Returns a human-readable file size string. */
  val formattedSize: String
    get() =
        when {
          sizeBytes < 1024 -> "$sizeBytes B"
          sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
          else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
}
