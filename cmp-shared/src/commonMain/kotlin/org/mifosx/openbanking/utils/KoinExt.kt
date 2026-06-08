/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.utils

import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.mifosx.openbanking.di.KoinModules

/** Koin qualifier for the platform-supplied app version string consumed by About and Settings. */
const val APP_VERSION_QUALIFIER = "appVersion"

private fun appVersionModule(appVersion: String) = module {
    single(named(APP_VERSION_QUALIFIER)) { appVersion }
}

fun koinConfiguration() = koinApplication {
    properties(KoinModules.koinProperties)
    modules(KoinModules.allModules)
}

/**
 * @param appVersion the real build version supplied by each platform entry point (Android from
 * the package manager, iOS from the bundle, desktop from jpackage); blank when a platform has no
 * version scheme yet, in which case consumers fall back to their own default.
 */
fun initKoin(appVersion: String = "", config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        properties(KoinModules.koinProperties)
        modules(KoinModules.allModules + appVersionModule(appVersion))
    }
}
