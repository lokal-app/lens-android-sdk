package com.lokalapps.lens.internal.data.model

/**
 * Represents information about a table in a SQLite database.
 *
 * Used to display table listings in the Database Inspector with metadata useful for understanding
 * table size and structure.
 *
 * @property name The table name
 * @property rowCount Approximate number of rows in the table
 * @property columnCount Number of columns in the table schema
 * @property columns Detailed column information (populated on demand)
 */
data class TableInfo(
    val name: String,
    val rowCount: Long,
    val columnCount: Int,
    val columns: List<ColumnInfo> = emptyList()
) {
  /** Returns true if this is a SQLite system table. */
  val isSystemTable: Boolean
    get() = name.startsWith("sqlite_") || name.startsWith("android_")

  /** Returns true if this is a Room metadata table. */
  val isRoomMetadataTable: Boolean
    get() = name == "room_master_table" || name.startsWith("room_")
}
