package com.lokalapps.lens.internal.analytics

import com.lokalapps.lens.internal.data.model.AnalyticsLogEntry
import com.lokalapps.lens.internal.data.model.UserPropertyEntry

/**
 * Validates analytics events and user properties against Firebase Analytics limits.
 *
 * Firebase silently drops or truncates data that violates its limits — no error is returned to the
 * caller. This validator surfaces those violations in Lens so developers can catch them before data
 * is lost in production.
 *
 * Only runs for entries that target FIREBASE. MoEngage, Adjust, etc. are not validated here.
 *
 * Sources: https://support.google.com/firebase/answer/9237506
 * https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics
 */
internal object FirebaseAnalyticsValidator {

  fun validateEvent(entry: AnalyticsLogEntry): EventValidationResult {
    if (!entry.destinations.contains("FIREBASE")) return EventValidationResult.CLEAN

    val eventViolations = validateEventName(entry.eventName)
    val tooManyParams =
        if (entry.params.size > MAX_PARAMS_PER_EVENT)
            listOf(Violation.TooManyParams(actual = entry.params.size, max = MAX_PARAMS_PER_EVENT))
        else emptyList()

    val paramViolations =
        entry.params
            .mapValues { (key, value) -> validateParam(key, value) }
            .filterValues { it.isNotEmpty() }

    return EventValidationResult(
        eventNameViolations = eventViolations,
        tooManyParamsViolation = tooManyParams,
        paramViolations = paramViolations,
    )
  }

  fun validateUserProperty(entry: UserPropertyEntry): PropertyValidationResult {
    if (!entry.destinations.contains("FIREBASE")) return PropertyValidationResult.CLEAN

    val propertyViolations =
        entry.properties
            .mapValues { (key, value) -> validateUserPropertyPair(key, value) }
            .filterValues { it.isNotEmpty() }

    return PropertyValidationResult(propertyViolations = propertyViolations)
  }

  // -------------------------------------------------------------------------
  // Event name rules
  // -------------------------------------------------------------------------

  private fun validateEventName(name: String): List<Violation> {
    val violations = mutableListOf<Violation>()
    if (name.length > MAX_EVENT_NAME_LENGTH)
        violations += Violation.NameTooLong(actual = name.length, max = MAX_EVENT_NAME_LENGTH)
    if (!NAME_REGEX.matches(name)) violations += Violation.InvalidCharacters(name)
    RESERVED_PREFIXES.firstOrNull { name.startsWith(it) }
        ?.let { violations += Violation.ReservedPrefix(it) }
    if (name in RESERVED_EVENT_NAMES) violations += Violation.ReservedName(name)
    return violations
  }

  // -------------------------------------------------------------------------
  // Parameter rules
  // -------------------------------------------------------------------------

  private fun validateParam(key: String, value: Any?): List<Violation> {
    val violations = mutableListOf<Violation>()
    if (key.length > MAX_PARAM_NAME_LENGTH)
        violations += Violation.NameTooLong(actual = key.length, max = MAX_PARAM_NAME_LENGTH)
    if (!NAME_REGEX.matches(key)) violations += Violation.InvalidCharacters(key)
    RESERVED_PREFIXES.firstOrNull { key.startsWith(it) }
        ?.let { violations += Violation.ReservedPrefix(it) }
    if (key in RESERVED_PARAM_NAMES) violations += Violation.ReservedName(key)
    if (value is String && value.length > MAX_PARAM_VALUE_LENGTH)
        violations += Violation.ValueTooLong(actual = value.length, max = MAX_PARAM_VALUE_LENGTH)
    return violations
  }

  // -------------------------------------------------------------------------
  // User property rules
  // -------------------------------------------------------------------------

  private fun validateUserPropertyPair(key: String, value: Any?): List<Violation> {
    val violations = mutableListOf<Violation>()
    if (key.length > MAX_USER_PROP_NAME_LENGTH)
        violations += Violation.NameTooLong(actual = key.length, max = MAX_USER_PROP_NAME_LENGTH)
    if (!NAME_REGEX.matches(key)) violations += Violation.InvalidCharacters(key)
    RESERVED_PREFIXES.firstOrNull { key.startsWith(it) }
        ?.let { violations += Violation.ReservedPrefix(it) }
    if (key in RESERVED_USER_PROPERTY_NAMES) violations += Violation.ReservedName(key)
    if (value is String && value.length > MAX_USER_PROP_VALUE_LENGTH)
        violations +=
            Violation.ValueTooLong(actual = value.length, max = MAX_USER_PROP_VALUE_LENGTH)
    return violations
  }

  // -------------------------------------------------------------------------
  // Limits (from Firebase docs)
  // -------------------------------------------------------------------------

  const val MAX_EVENT_NAME_LENGTH = 40
  const val MAX_PARAM_NAME_LENGTH = 40
  const val MAX_PARAM_VALUE_LENGTH = 100
  const val MAX_PARAMS_PER_EVENT = 25
  const val MAX_USER_PROP_NAME_LENGTH = 24
  const val MAX_USER_PROP_VALUE_LENGTH = 36

  private val NAME_REGEX = Regex("[a-zA-Z][a-zA-Z0-9_]*")

  private val RESERVED_PREFIXES = listOf("firebase_", "ga_", "google_", "gtag_")

  // https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event
  private val RESERVED_EVENT_NAMES =
      setOf(
          "ad_activeview",
          "ad_click",
          "ad_exposure",
          "ad_impression",
          "ad_query",
          "ad_reward",
          "adunit_exposure",
          "app_background",
          "app_clear_data",
          "app_exception",
          "app_open",
          "app_remove",
          "app_store_refund",
          "app_store_subscription_cancel",
          "app_store_subscription_convert",
          "app_store_subscription_renew",
          "app_update",
          "app_upgrade",
          "dynamic_link_app_open",
          "dynamic_link_app_update",
          "dynamic_link_first_open",
          "error",
          "first_open",
          "first_visit",
          "in_app_purchase",
          "notification_dismiss",
          "notification_foreground",
          "notification_open",
          "notification_receive",
          "notification_send",
          "os_update",
          "session_start",
          "session_start_with_rollout",
          "user_engagement",
          // Recommended events (reserved but usable with correct params)
          "add_payment_info",
          "add_shipping_info",
          "add_to_cart",
          "add_to_wishlist",
          "begin_checkout",
          "campaign_details",
          "earn_virtual_currency",
          "generate_lead",
          "join_group",
          "level_end",
          "level_start",
          "level_up",
          "login",
          "post_score",
          "purchase",
          "refund",
          "remove_from_cart",
          "screen_view",
          "search",
          "select_content",
          "select_item",
          "select_promotion",
          "share",
          "sign_up",
          "spend_virtual_currency",
          "tutorial_begin",
          "tutorial_complete",
          "unlock_achievement",
          "view_cart",
          "view_item",
          "view_item_list",
          "view_promotion",
          "view_search_results",
      )

  // https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Param
  private val RESERVED_PARAM_NAMES =
      setOf(
          "cid",
          "currency",
          "customer_id",
          "customerid",
          "dclid",
          "gclid",
          "session_id",
          "sessionid",
          "sfmc_id",
          "sid",
          "srsltid",
          "uid",
          "user_id",
          "userid",
          "firebase_conversion",
      )

  // https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.UserProperty
  private val RESERVED_USER_PROPERTY_NAMES = setOf("Age", "Gender", "Interest")
}

// -------------------------------------------------------------------------
// Result types
// -------------------------------------------------------------------------

internal data class EventValidationResult(
    val eventNameViolations: List<Violation>,
    val tooManyParamsViolation: List<Violation>,
    val paramViolations: Map<String, List<Violation>>,
) {
  val hasViolations: Boolean
    get() =
        eventNameViolations.isNotEmpty() ||
            tooManyParamsViolation.isNotEmpty() ||
            paramViolations.isNotEmpty()

  companion object {
    val CLEAN =
        EventValidationResult(
            eventNameViolations = emptyList(),
            tooManyParamsViolation = emptyList(),
            paramViolations = emptyMap(),
        )
  }
}

internal data class PropertyValidationResult(
    val propertyViolations: Map<String, List<Violation>>,
) {
  val hasViolations: Boolean
    get() = propertyViolations.isNotEmpty()

  companion object {
    val CLEAN = PropertyValidationResult(propertyViolations = emptyMap())
  }
}

internal sealed class Violation {
  data class NameTooLong(val actual: Int, val max: Int) : Violation() {
    override fun message() = "name is $actual chars (max $max) — Firebase will drop this"
  }

  data class InvalidCharacters(val value: String) : Violation() {
    override fun message() =
        "\"$value\" contains invalid characters — must match [a-zA-Z][a-zA-Z0-9_]*"
  }

  data class ReservedPrefix(val prefix: String) : Violation() {
    override fun message() = "name starts with reserved prefix \"$prefix\""
  }

  data class ReservedName(val name: String) : Violation() {
    override fun message() = "\"$name\" is a reserved Firebase name"
  }

  data class TooManyParams(val actual: Int, val max: Int) : Violation() {
    override fun message() = "$actual params (max $max) — Firebase drops extras"
  }

  data class ValueTooLong(val actual: Int, val max: Int) : Violation() {
    override fun message() = "value is $actual chars (max $max) — Firebase will truncate"
  }

  abstract fun message(): String
}
