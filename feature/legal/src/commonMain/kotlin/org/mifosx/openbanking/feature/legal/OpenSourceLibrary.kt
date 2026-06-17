/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.legal

/**
 * A third-party open-source dependency acknowledged on the Licenses screen.
 *
 * @property name Display name of the library as published by its maintainer.
 * @property author Maintaining organisation or primary author credited for the library.
 * @property license SPDX-style license identifier under which the library is distributed.
 * @property url Homepage or source repository where the full license text can be obtained.
 */
internal data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val url: String,
)

/**
 * Open-source libraries bundled with Mifos X Open Banking, mirroring the dependency catalog
 * in gradle/libs.versions.toml. Entries are declared in license order (Apache-2.0, MIT,
 * MPL-2.0, EPL-1.0) so that grouping by [OpenSourceLibrary.license] yields a stable section
 * sequence on the Licenses screen.
 */
internal val OPEN_SOURCE_LIBRARIES: List<OpenSourceLibrary> = listOf(
    OpenSourceLibrary(
        name = "Kotlin",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/JetBrains/kotlin",
    ),
    OpenSourceLibrary(
        name = "kotlinx.coroutines",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    OpenSourceLibrary(
        name = "kotlinx.serialization",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization",
    ),
    OpenSourceLibrary(
        name = "kotlinx-datetime",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx-datetime",
    ),
    OpenSourceLibrary(
        name = "kotlinx.collections.immutable",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.collections.immutable",
    ),
    OpenSourceLibrary(
        name = "Compose Multiplatform",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/JetBrains/compose-multiplatform",
    ),
    OpenSourceLibrary(
        name = "AndroidX Lifecycle",
        author = "Google & JetBrains",
        license = "Apache-2.0",
        url = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
    ),
    OpenSourceLibrary(
        name = "AndroidX Navigation Compose",
        author = "Google & JetBrains",
        license = "Apache-2.0",
        url = "https://developer.android.com/jetpack/androidx/releases/navigation",
    ),
    OpenSourceLibrary(
        name = "AndroidX Room",
        author = "Google",
        license = "Apache-2.0",
        url = "https://developer.android.com/jetpack/androidx/releases/room",
    ),
    OpenSourceLibrary(
        name = "Ktor",
        author = "JetBrains",
        license = "Apache-2.0",
        url = "https://github.com/ktorio/ktor",
    ),
    OpenSourceLibrary(
        name = "Ktorfit",
        author = "Jens Klingenberg",
        license = "Apache-2.0",
        url = "https://github.com/Foso/Ktorfit",
    ),
    OpenSourceLibrary(
        name = "Koin",
        author = "Kotzilla & Koin contributors",
        license = "Apache-2.0",
        url = "https://github.com/InsertKoinIO/koin",
    ),
    OpenSourceLibrary(
        name = "Store5",
        author = "Mobile Native Foundation",
        license = "Apache-2.0",
        url = "https://github.com/MobileNativeFoundation/Store",
    ),
    OpenSourceLibrary(
        name = "Coil",
        author = "Coil Contributors",
        license = "Apache-2.0",
        url = "https://github.com/coil-kt/coil",
    ),
    OpenSourceLibrary(
        name = "Multiplatform Settings",
        author = "Russell Wolf",
        license = "Apache-2.0",
        url = "https://github.com/russhwolf/multiplatform-settings",
    ),
    OpenSourceLibrary(
        name = "Okio",
        author = "Square",
        license = "Apache-2.0",
        url = "https://github.com/square/okio",
    ),
    OpenSourceLibrary(
        name = "Kermit",
        author = "Touchlab",
        license = "Apache-2.0",
        url = "https://github.com/touchlab/Kermit",
    ),
    OpenSourceLibrary(
        name = "AboutLibraries",
        author = "Mike Penz",
        license = "Apache-2.0",
        url = "https://github.com/mikepenz/AboutLibraries",
    ),
    OpenSourceLibrary(
        name = "Compottie",
        author = "Alexander Zhirkevich",
        license = "MIT",
        url = "https://github.com/alexzhirkevich/compottie",
    ),
    OpenSourceLibrary(
        name = "kmp-project-template",
        author = "Mifos Initiative",
        license = "MPL-2.0",
        url = "https://github.com/openMF/kmp-project-template",
    ),
    OpenSourceLibrary(
        name = "JUnit 4",
        author = "JUnit Team",
        license = "EPL-1.0",
        url = "https://github.com/junit-team/junit4",
    ),
)
