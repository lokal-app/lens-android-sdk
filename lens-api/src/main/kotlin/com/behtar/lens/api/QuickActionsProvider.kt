package com.behtar.lens.api

/**
 * Provider interface for quick actions.
 *
 * Implement this interface to add app-specific debugging shortcuts to Lens. Quick actions provide
 * one-tap access to common debugging operations like clearing cache, forcing crashes, simulating
 * conditions, etc.
 *
 * **Example Implementation:**
 *
 * ```kotlin
 * class MyQuickActionsProvider(
 *     private val context: Context,
 *     private val cacheManager: CacheManager,
 *     private val userRepository: UserRepository
 * ) : QuickActionsProvider {
 *
 *     override fun getActions(): List<QuickAction> = listOf(
 *         QuickAction(
 *             id = "clear_cache",
 *             name = "Clear Cache",
 *             description = "Clears all cached data including images and API responses",
 *             icon = R.drawable.ic_clear,
 *             action = {
 *                 cacheManager.clearAll()
 *                 Toast.makeText(context, "Cache cleared!", Toast.LENGTH_SHORT).show()
 *             }
 *         )
 *     )
 * }
 * ```
 *
 * **Registration:**
 *
 * ```kotlin
 * Lens.install(app) {
 *     enabled = true
 *     quickActions(MyQuickActionsProvider(app, cacheManager, userRepository))
 * }
 * ```
 */
interface QuickActionsProvider {

  /**
   * Returns the list of available quick actions. These are displayed as tappable items in the Quick
   * Actions plugin.
   *
   * @return List of [QuickAction] objects
   */
  fun getActions(): List<QuickAction>
}

/**
 * Represents a quick action shortcut.
 *
 * @property id Unique identifier for this action
 * @property name Display name for the action
 * @property description Brief explanation of what this action does
 * @property icon Icon resource ID for display (use 0 if no icon)
 * @property action Lambda to execute when the action is triggered
 * @property isDestructive If true, the action will be highlighted as potentially dangerous
 */
data class QuickAction(
    val id: String,
    val name: String,
    val description: String,
    val icon: Int,
    val action: () -> Unit,
    val isDestructive: Boolean = false
)
