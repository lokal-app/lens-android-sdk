package com.lokalapps.lens.api

/**
 * Marks Lens SDK declarations that are **experimental**.
 *
 * Experimental APIs may change or be removed in future minor versions without notice. Callers must
 * opt in with `@OptIn(LensExperimental::class)` to acknowledge this instability.
 *
 * Features currently marked experimental:
 * - [ViewLensPlugin] — View-based plugin rendering for non-Compose consumers
 * - [HeaderRedactor] — Network header redaction
 * - Global search, performance monitoring, ANR detection, and data export internals
 */
@RequiresOptIn(
    message =
        "This Lens API is experimental and may change in future versions. " +
            "Use @OptIn(LensExperimental::class) to suppress this warning.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class LensExperimental
