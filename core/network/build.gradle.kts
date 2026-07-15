/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
import java.util.Properties

plugins {
    alias(libs.plugins.kmp.library.convention)
    alias(libs.plugins.ktrofit)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.mifosx.openbanking.core.network"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

// ── Generate HsbcConfig.kt from local.properties (our own reader, not BuildKonfig) ──
val hsbcConfigOutputDir = layout.buildDirectory.dir("generated/hsbcconfig/commonMain/kotlin")

val generateHsbcConfig = tasks.register("generateHsbcConfig") {
    val localPropsFile = rootProject.file("local.properties")
    val outputDir = hsbcConfigOutputDir
    inputs.file(localPropsFile).optional(true)
    outputs.dir(outputDir)
    doLast {
        if (!localPropsFile.exists()) {
            throw GradleException(
                "local.properties not found at ${localPropsFile.absolutePath}. " +
                    "Add the HSBC sandbox keys: HSBC_CLIENT_ID, HSBC_KID, HSBC_SOFTWARE_STATEMENT, " +
                    "HSBC_BANK_HOST, HSBC_REDIRECT_URI.",
            )
        }
        val props = Properties()
        localPropsFile.inputStream().use { props.load(it) }

        val requiredKeys = listOf(
            "HSBC_CLIENT_ID",
            "HSBC_KID",
            "HSBC_SOFTWARE_STATEMENT",
            "HSBC_BANK_HOST",
            "HSBC_REDIRECT_URI",
        )
        val missing = requiredKeys.filter { props.getProperty(it).isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing or blank required key(s) in local.properties: ${missing.joinToString()}",
            )
        }

        // Escape for safe embedding in a Kotlin "..." literal: backslash first, then $ (template), then quote.
        fun value(key: String): String =
            props.getProperty(key)!!.trim()
                .replace("\\", "\\\\")
                .replace("\$", "\\\$")
                .replace("\"", "\\\"")

        val pkgDir = outputDir.get().asFile.resolve("org/mifosx/openbanking/core/network/config")
        pkgDir.mkdirs()
        pkgDir.resolve("HsbcConfig.kt").writeText(
            """
            |/*
            | * GENERATED from local.properties by the :core:network `generateHsbcConfig` task.
            | * DO NOT EDIT — changes will be overwritten on the next build.
            | */
            |package org.mifosx.openbanking.core.network.config
            |
            |internal object HsbcConfig {
            |    const val CLIENT_ID: String = "${value("HSBC_CLIENT_ID")}"
            |    const val KID: String = "${value("HSBC_KID")}"
            |    const val SOFTWARE_STATEMENT: String = "${value("HSBC_SOFTWARE_STATEMENT")}"
            |    const val BANK_HOST: String = "${value("HSBC_BANK_HOST")}"
            |    const val REDIRECT_URI: String = "${value("HSBC_REDIRECT_URI")}"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateHsbcConfig)
            dependencies {
                api(projects.core.common)
                api(projects.core.model)
                api(projects.coreBase.network)

                implementation(projects.core.datastore)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.serialization.kotlinx.json)

                implementation(libs.ktorfit.lib)

                implementation(libs.squareup.okio)

                implementation(libs.cryptography.core)
                implementation(libs.cryptography.provider.optimal)

                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.serialization)
                implementation(libs.multiplatform.settings.coroutines)

                implementation(compose.runtime)
                implementation(libs.components.resources)

                api(libs.kermit.logging)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    publicResClass = false
    generateResClass = always
    packageOfResClass = "org.mifosx.openbanking.core.network.generated.resources"
}

dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspJs", libs.ktorfit.ksp)
    add("kspWasmJs", libs.ktorfit.ksp)
    add("kspDesktop", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
}
