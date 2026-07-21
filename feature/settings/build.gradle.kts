/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    alias(libs.plugins.cmp.feature.convention)
    alias(libs.plugins.aboutLibraries)
}

android {
    namespace = "org.mifosx.openbanking.feature.settings"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.data)
            implementation(projects.core.model)
            implementation(projects.core.ui)
            implementation(libs.kotlinx.coroutines.core)

            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.aboutlibraries.compose.m3)
        }

        commonTest.dependencies {
            @OptIn(ExperimentalComposeLibrary::class)
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
 * The commonTest Compose UI class also compiles into androidUnitTest, where there is no
 * Robolectric runner to stand up a composition — every case there NPEs. It is meant for the
 * desktop runner, so the JVM unit-test tasks skip it.
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
        packageOfResClass = "org.mifosx.openbanking.feature.settings.generated.resources"
    }
}

/**
 * The generated open-source licence catalogue, written into this module's Compose resources so the
 * Licences screen can read it at runtime through `Res.readBytes("files/aboutlibraries.json")`.
 */
val aboutLibrariesOutput = layout.projectDirectory.file(
    "src/commonMain/composeResources/files/aboutlibraries.json",
)

aboutLibraries {
    export {
        outputFile.set(aboutLibrariesOutput)
        prettyPrint.set(true)
    }
}

/**
 * Regenerates the catalogue before the Compose resource pipeline copies this module's `files/`, so a
 * fresh build always packages an up-to-date `aboutlibraries.json` rather than a stale checked-in
 * copy. The variant-less `exportLibraryDefinitions` collects the full transitive graph, and the
 * copy/prepare tasks that consume `composeResources` are the ones that read the generated file.
 */
val composeResourceConsumers = listOf(
    "copyNonXmlValueResources",
    "convertXmlValueResources",
    "prepareComposeResourcesTask",
    "generateResourceAccessors",
    "generateComposeResClass",
)
tasks.matching { task -> composeResourceConsumers.any { task.name.startsWith(it) } }.configureEach {
    dependsOn("exportLibraryDefinitions")
}
