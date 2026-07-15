/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module that loads the HSBC certificate material **synchronously** from the
 * platform's native resource store (Android assets, JVM classpath, iOS bundle) and provides:
 * - `MtlsIdentity` — the mTLS client identity (PKCS#12 bytes).
 * - a `named("hsbcSigningKey")` `String` — the `private_key_jwt` signing key (PEM).
 *
 * Because the certificate is read here (not through the suspend `composeResources` API), the client
 * factory and DI graph stay fully synchronous. Web (js/wasmJs) provides nothing — a browser cannot
 * present a client certificate.
 */
expect val networkPlatformModule: Module
