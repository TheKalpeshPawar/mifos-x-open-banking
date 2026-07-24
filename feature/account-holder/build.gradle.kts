/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
plugins {
    alias(libs.plugins.cmp.feature.convention)
}

android {
    namespace = "org.mifosx.openbanking.feature.accountholder"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.data)
            implementation(projects.core.model)
            implementation(projects.core.ui)
            implementation(projects.coreBase.store)
            implementation(projects.coreBase.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(projects.core.network)

            // Drives AccountHolderScreenUiTest headlessly on the JVM. The Robolectric and instrumented
            // suites cover the same surfaces against the real Android renderer.
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        desktopTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }

        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.bundles.androidx.compose.ui.test)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.bundles.androidx.compose.ui.test)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.espresso.core)
        }
    }
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * `AccountHolderScreenUiTest` lives in commonTest so it also compiles for the native and desktop targets,
 * but the Android unit-test compilation inherits it too — and there it has no Robolectric runner to
 * supply an Android runtime, so every case would NPE. Excluding it from the `*UnitTest` tasks keeps
 * the Android side to `AccountHolderScreenRobolectricTest`, which does declare a runner.
 */
tasks.withType<Test>().configureEach {
    if (name.endsWith("UnitTest")) {
        filter {
            excludeTestsMatching("*ScreenUiTest")
            isFailOnNoMatchingTests = false
        }
    }
}

compose {
    resources {
        packageOfResClass = "org.mifosx.openbanking.feature.accountholder.generated.resources"
    }
}
