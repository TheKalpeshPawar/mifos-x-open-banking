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

import kotlin.test.Test
import kotlin.test.assertTrue

class OpenSourceLibrariesTest {

    @Test
    fun listIsNonEmptyAndComplete() {
        assertTrue(OPEN_SOURCE_LIBRARIES.size >= 15)
        OPEN_SOURCE_LIBRARIES.forEach { lib ->
            assertTrue(lib.name.isNotBlank(), "blank name")
            assertTrue(lib.author.isNotBlank(), "blank author for ${lib.name}")
            assertTrue(lib.license.isNotBlank(), "blank license for ${lib.name}")
            assertTrue(lib.url.startsWith("https://"), "bad url for ${lib.name}")
        }
    }

    @Test
    fun noDuplicateLibraries() {
        val names = OPEN_SOURCE_LIBRARIES.map { it.name }
        assertTrue(names.size == names.distinct().size, "duplicate library names")
    }

    @Test
    fun coreStackIsAcknowledged() {
        val names = OPEN_SOURCE_LIBRARIES.joinToString(" ") { it.name }
        listOf("Kotlin", "Compose", "Ktor", "Koin", "Room").forEach { expected ->
            assertTrue(expected in names, "$expected missing from license list")
        }
    }
}
