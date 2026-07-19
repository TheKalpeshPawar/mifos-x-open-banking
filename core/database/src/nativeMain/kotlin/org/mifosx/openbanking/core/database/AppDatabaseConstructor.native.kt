/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database

import androidx.room3.RoomDatabaseConstructor

/**
 * Hand-written `actual` for [AppDatabaseConstructor] on the Apple/native targets.
 *
 * Room's KSP compiler emits this bridge automatically for every target whose KSP task runs. On a
 * non-macOS host the native KSP tasks are skipped (KSP-for-native and linking are mac-only), so the
 * generated `actual` — and its `AppDatabase_Impl` — are absent and the shared `expect` fails to
 * resolve, breaking the iOS Kotlin metadata compile that still runs on Linux/CI.
 *
 * This stub satisfies the `expect`/`actual` contract so that compile succeeds. Its body is never
 * reached on a non-mac build: the iOS application is assembled only on macOS, where Room's generated
 * `AppDatabaseConstructor` supplies the real instance via `AppDatabase_Impl`.
 */
actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase =
        error("AppDatabase is provided by Room's generated code on Apple builds (macOS only)")
}
