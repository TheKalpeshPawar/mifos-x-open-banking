package org.convention

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the `kover` plugin with the [configure] lambda.
 *
 * Mirrors the [detektGradle] / [spotlessGradle] helpers in ProjectExtensions.kt
 * so KoverConventionPlugin reads the same way as DetektConventionPlugin and
 * SpotlessConventionPlugin.
 */
internal inline fun Project.koverGradle(crossinline configure: KoverProjectExtension.() -> Unit) =
    extensions.configure<KoverProjectExtension> {
        configure()
    }

/**
 * Report filters — single source of truth for what coverage ignores.
 *
 * Applied by KoverConventionPlugin to **every** project, root and leaf. This matters
 * because CI's per-module coverage floor reads each module's own `koverXmlReport`, not
 * the root aggregate: configuring the filters only on root left every leaf module's
 * extension unconfigured, so DI classes, generated code and @Composable functions were
 * counted against the gating numbers and every module's coverage read low.
 */
internal fun Project.configureKoverFilters() = koverGradle {
    reports {
        filters {
            excludes {
                classes(
                    "*.di.*",                       // Koin / kotlin-inject DI modules
                    "*.BuildConfig",
                    "*ComposableSingletons*",       // Compose generated lambda holders
                    "*_*Factory*",                  // Generated factories
                    "*\$ComposableLambda\$*",
                    "*Preview*",                    // @Preview functions
                    "*Test*",                       // test helpers themselves
                )
                packages(
                    "*.generated.*",
                    "*.ksp.*",
                )
                annotatedBy(
                    // @Composable funcs are better tested via screenshot/UI tests,
                    // not Kover line coverage.
                    "androidx.compose.runtime.Composable",
                )
            }
        }
    }
}

/**
 * Root-level verify rule for the aggregated coverage report.
 *
 * Root-only by design: applying this per module would fail every module that currently
 * sits below the threshold. Per-module floors are enforced separately in CI from
 * `.kover-floor.yml`.
 */
internal fun Project.configureKoverVerify() = koverGradle {
    reports {
        verify {
            // Phase 1 floor — single global threshold while coverage grows.
            // Per-module thresholds added as test-coverage PRs raise individual modules.
            rule { minBound(40) }
        }
    }
}
