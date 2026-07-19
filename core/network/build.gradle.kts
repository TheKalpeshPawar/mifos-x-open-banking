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
//
// This is a MANUAL-ONLY task: it must never run as part of a normal build/compile.
// It writes secrets (client id, transport passphrase, ...) into a generated Kotlin
// file, so its output lives outside `build/` (which gets wiped by `clean`) at a
// stable, gitignored path: core/network/generated/hsbcconfig/commonMain/kotlin.
// The Kotlin source set below adds that PATH as a plain source directory — never
// the task or its TaskProvider — so Gradle does not wire an implicit
// compile-depends-on-generate relationship. Run `./gradlew :core:network:generateHsbcConfig`
// by hand (or after `clean`/deleting the generated/ dir) before building.
val hsbcConfigOutputDir = layout.projectDirectory.dir("generated/hsbcconfig/commonMain/kotlin")

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
                    "HSBC_BANK_HOST, HSBC_REDIRECT_URI, HSBC_TRANSPORT_P12_PASSWORD.",
            )
        }
        val props = Properties()
        localPropsFile.inputStream().use { props.load(it) }

        val requiredKeys = listOf(
            "HSBC_CLIENT_ID",
            "HSBC_KID",
            "HSBC_SOFTWARE_STATEMENT",
            "HSBC_BANK_HOST",
            "HSBC_AUTHORIZE_HOST",
            "HSBC_REDIRECT_URI",
            "HSBC_TRANSPORT_P12_PASSWORD",
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

        val pkgDir = outputDir.asFile.resolve("org/mifosx/openbanking/core/network/config")
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
            |
            |    /**
            |     * The OIDC authorization host — DISTINCT from [BANK_HOST]. Token + consent calls go to
            |     * the mTLS host (`secure.sandbox.ob.hsbc.co.uk`); the front-channel authorize URL the
            |     * browser opens is served from `sandbox.ob.hsbc.co.uk`. Sending the browser to the mTLS
            |     * host (or, as the pre-fix bug did, to a host-less relative URL that resolves to
            |     * `http://localhost`) means the authorization page never loads.
            |     */
            |    const val AUTHORIZE_HOST: String = "${value("HSBC_AUTHORIZE_HOST")}"
            |    const val REDIRECT_URI: String = "${value("HSBC_REDIRECT_URI")}"
            |
            |    /**
            |     * The PKCS#12 export passphrase protecting [org.mifosx.openbanking.core.network.certs
            |     * .CertPaths.TRANSPORT_P12]. Required, and required to be non-empty: Android's
            |     * BouncyCastle refuses to run PBKDF2 on a zero-length password for a PBES2-encrypted
            |     * bundle (`IllegalArgumentException: password empty`), while the JVM accepts it — so an
            |     * empty passphrase builds and passes on desktop, then crashes every Android launch.
            |     */
            |    const val TRANSPORT_P12_PASSWORD: String = "${value("HSBC_TRANSPORT_P12_PASSWORD")}"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    sourceSets {
        commonMain {
            // Plain path, NOT the task/TaskProvider — see the comment above
            // generateHsbcConfig's registration for why this must stay a path.
            kotlin.srcDir(hsbcConfigOutputDir)
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

                api(libs.kermit.logging)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // TestSigningKey generates a throwaway PS256 key per run, so no signing material is
            // committed. Declared explicitly rather than leaning on the test compilation's
            // association with commonMain.
            implementation(libs.cryptography.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)

            // FAPI signs the private_key_jwt client assertion with PS256 (RSA-PSS). Android's own JCA
            // providers expose PSS only as `SHA256withRSA/PSS` (AndroidOpenSSL) and never register the
            // standard name `RSASSA-PSS`, which is what the JDK cryptography provider asks for — so
            // signPs256 dies with NoSuchAlgorithmException on device while passing on desktop. This
            // artifact is a drop-in with no API surface: it contributes a ServiceLoader
            // `DefaultJdkSecurityProvider` that hands BouncyCastle to the JDK provider, which does
            // register RSASSA-PSS. It does NOT touch the global JCE provider list, so mTLS keeps
            // loading its PKCS#12 through Android's own BouncyCastle. Upstream documents this exact
            // case: RSA-PSS is "Not available on Android; use BouncyCastle".
            implementation(libs.cryptography.provider.jdk.bc)
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
        }

        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        desktopTest.dependencies {
            // HeldCertificate mints a throwaway self-signed mTLS identity per run, so no PKCS#12 is
            // committed. okhttp itself is already here transitively via ktor-client-okhttp.
            implementation(libs.okhttp.tls)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
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
