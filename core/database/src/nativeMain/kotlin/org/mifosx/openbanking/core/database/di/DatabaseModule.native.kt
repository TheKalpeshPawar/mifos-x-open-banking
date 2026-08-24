/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.migration.addAppMigrations
import template.core.base.database.AppDatabaseFactory

actual val platformModule: Module = module {
    single {
        AppDatabaseFactory()
            .createDatabase<AppDatabase>(
                databaseName = AppDatabase.DATABASE_NAME,
            )
            .addAppMigrations()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }
}
