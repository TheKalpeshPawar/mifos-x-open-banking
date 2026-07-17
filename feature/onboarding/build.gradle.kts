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
    namespace = "org.mifosx.openbanking.feature.onboarding"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.model)
            implementation(projects.coreBase.ui)
            implementation(projects.coreBase.platform)

            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
        }

        desktopTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
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

dependencies {
    add("androidUnitTestImplementation", libs.junit)
    add("androidUnitTestImplementation", libs.robolectric)
    add("androidUnitTestImplementation", libs.androidx.compose.ui.test)
    add("androidUnitTestImplementation", libs.koin.test)
}

compose {
    resources {
        packageOfResClass = "org.mifosx.openbanking.feature.onboarding.generated.resources"
    }
}
