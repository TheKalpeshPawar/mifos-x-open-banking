/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package local

import com.mobilebytelabs.kmpflavors.KmpFlavorExtension
import org.gradle.api.Project
import java.util.Properties

/**
 * Consumer-app flavor extension for **mifos-x-open-banking** (NOT synced — survives
 * every `sync-dirs.sh` from the upstream template).
 *
 * Adds a `userType` dimension orthogonal to the synced `contentType` (demo/prod):
 *
 * | Dimension     | Flavors                       | Default    |
 * |---------------|-------------------------------|------------|
 * | contentType   | demo, prod (synced base)      | demo       |
 * | userType      | consumer, fieldOfficer        | consumer   |
 *
 * → matrix: 2 (contentType) × 2 (userType) × 3 (buildType) = 12 variants.
 *
 * `USER_SURFACE` is exposed on the generated `BuildKonfig` so the app-shell can pick
 * the flavor-appropriate bottom-nav at runtime (Phase 2 — consumer: Home/Accounts/Pay/
 * Cards/More · fieldOfficer: Dashboard/Customers/Applications/Messages/More).
 *
 * See `docs/FLAVORS_EXTENSION.md`.
 */
object LocalFlavors {
    @JvmStatic
    fun apply(ext: KmpFlavorExtension, project: Project) {
        val obpConsumerKey = readLocalProperty(project, "obp.consumer.key")

        ext.flavorDimensions.register("userType") { priority.set(1) }

        ext.flavors.register("consumer") {
            dimension.set("userType")
            isDefault.set(true)
            applicationIdSuffix.set(".consumer")
            buildConfigField("String", "USER_SURFACE", "\"consumer\"")
            buildConfigField("String", "OBP_CONSUMER_KEY", "\"$obpConsumerKey\"")
        }

        ext.flavors.register("fieldOfficer") {
            dimension.set("userType")
            applicationIdSuffix.set(".fieldofficer")
            buildConfigField("String", "USER_SURFACE", "\"fieldOfficer\"")
            buildConfigField("String", "OBP_CONSUMER_KEY", "\"$obpConsumerKey\"")
        }
    }

    /** Reads a key from the project's gitignored root local.properties; "" if absent. */
    private fun readLocalProperty(project: Project, key: String): String {
        val file = project.rootProject.file("local.properties")
        if (!file.exists()) return ""
        return file.inputStream().use { stream ->
            Properties().apply { load(stream) }.getProperty(key, "")
        }
    }
}
