/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store

import template.core.base.store.infra.StoreRegistry

/**
 * Application-level [StoreRegistry] — the single named-qualifier registry for every
 * `org.mobilenativefoundation.store.store5.Store` the app exposes.
 *
 * OBP banking stores are registered in Phase 3 (one Store5 qualifier per OBP service),
 * e.g.:
 *
 * ```kotlin
 * object AppStoreRegistry : StoreRegistry() {
 *     val Accounts     = store("accounts")
 *     val Transactions = store("transactions")
 * }
 * ```
 *
 * Then reference the qualifier from Koin DI in [appStoreModule]:
 *
 * ```kotlin
 * single<Store<AccountId, Account>>(qualifier = AppStoreRegistry.Accounts) { ... }
 * ```
 *
 * Centralizing here gives a one-place audit of every Store the app owns and prevents
 * qualifier-name collisions across feature modules.
 */
object AppStoreRegistry : StoreRegistry() {
    /** Authenticated user's account list (Store5, keyed by Unit). */
    val Accounts = store("accounts")

    /** Per-account transactions (Store5, keyed by accountId). */
    val Transactions = store("transactions")

    /** Per-account cards (Store5, keyed by accountId). */
    val Cards = store("cards")

    /** Current user's full card list across all accounts (Store5, keyed by Unit). */
    val UserCards = store("user-cards")

    /** Per-account counterparties / payees (Store5, keyed by accountId). */
    val Counterparties = store("counterparties")

    /** Bank customers, field-officer surface (Store5, keyed by Unit). */
    val Customers = store("customers")

    /** Account-application review queue (Store5, keyed by Unit). */
    val AccountApplications = store("account-applications")

    /** Per-customer message thread (Store5, keyed by customerId). */
    val CustomerMessages = store("customer-messages")

    /** Per-customer KYC documents (Store5, keyed by customerId). */
    val KycDocuments = store("kyc-documents")

    /** Bank products (Store5, keyed by Unit). */
    val Products = store("products")

    /** Bank ATM locations (Store5, keyed by Unit). */
    val Atms = store("atms")
}
