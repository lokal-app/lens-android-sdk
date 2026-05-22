package com.lokalapps.lens.internal.data.model

/**
 * Represents detailed schema information about a database column.
 *
 * This data comes from SQLite's `PRAGMA table_info()` command and provides all metadata needed to
 * understand the column structure.
 *
 * @property position Column index (0-based) in the table
 * @property name Column name
 * @property type Declared type (INTEGER, TEXT, REAL, BLOB, or empty for untyped)
 * @property isNotNull Whether the column has a NOT NULL constraint
 * @property defaultValue Default value expression (null if none)
 * @property isPrimaryKey Whether this column is part of the primary key
 */
data class ColumnInfo(
    val position: Int,
    val name: String,
    val type: String,
    val isNotNull: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean
) {
  /** Returns a displayable type string, normalizing empty types to "ANY". */
  val displayType: String
    get() = type.ifBlank { "ANY" }

  /** Returns a description of the column constraints. */
  val constraintsDescription: String
    get() =
        buildString {
              if (isPrimaryKey) append("PRIMARY KEY")
              if (isNotNull) {
                if (isNotEmpty()) append(", ")
                append("NOT NULL")
              }
              if (defaultValue != null) {
                if (isNotEmpty()) append(", ")
                append("DEFAULT $defaultValue")
              }
            }
            .ifEmpty { "—" }
}
