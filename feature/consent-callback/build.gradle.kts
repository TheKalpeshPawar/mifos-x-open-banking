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
    namespace = "org.mifosx.openbanking.feature.consentcallback"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.model)
            implementation(projects.core.network)
            implementation(projects.coreBase.ui)
            implementation(projects.coreBase.network)
            implementation(projects.feature.onboarding)

            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
        }

        // Compose UI tests run headless on the JVM via runComposeUiTest — no device needed.
        //
        // These live in desktopTest rather than commonTest on purpose: a commonTest UI test is also
        // compiled into androidUnitTest, where there is no Robolectric runner to supply an Android
        // runtime, so every case NPEs. The Android equivalents are the Robolectric class below.
        desktopTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }

        // The same surfaces against the real Android Compose runtime, under Robolectric — still on
        // the JVM, so they run in CI and on a Linux dev box with no emulator.
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.bundles.androidx.compose.ui.test)
        }
    }
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

compose {
    resources {
        packageOfResClass = "org.mifosx.openbanking.feature.consentcallback.generated.resources"
    }
}
