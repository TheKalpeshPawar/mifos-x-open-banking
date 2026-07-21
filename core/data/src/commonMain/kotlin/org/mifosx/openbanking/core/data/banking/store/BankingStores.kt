/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import kotlinx.coroutines.flow.map
import org.mifosx.openbanking.core.data.banking.mapper.toAccountBalance
import org.mifosx.openbanking.core.data.banking.mapper.toAccountBalanceLines
import org.mifosx.openbanking.core.data.banking.mapper.toAccountDetail
import org.mifosx.openbanking.core.data.banking.mapper.toAccountEntity
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccount
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccounts
import org.mifosx.openbanking.core.data.banking.mapper.toDirectDebitsSummary
import org.mifosx.openbanking.core.data.banking.mapper.toStandingOrdersSummary
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionEntity
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItem
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItems
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.database.banking.dao.AccountDao
import org.mifosx.openbanking.core.database.banking.dao.TransactionDao
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.network.api.Aisp
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult
import template.core.base.store.infra.DefaultValidator
import template.core.base.store.infra.StoreFactory
import kotlin.time.Duration.Companion.minutes

/**
 * Builds the Store5 stores backing the home dashboard, reusing the framework's [StoreFactory]. Each
 * read store maps the OBIE payload to domain in the [Fetcher], persists it through a Room
 * [SourceOfTruth], and marks the TTL validator fresh after every successful fetch.
 *
 * The SourceOfTruth reader emits `null` for an empty table so a cold start reads as a cache miss
 * (Loading) rather than a premature empty state. The accounts store carries the whole consented set
 * under one key; balances and transactions are keyed by `AccountId`. Balances are volatile, so they
 * use an in-memory store with no Room persistence.
 *
 * The stores are wrapped by the banking repositories and never exposed above the data layer.
 */
object BankingStores {

    /** Single key for the accounts store — all consented accounts belong to one PSU session. */
    const val ACCOUNTS_KEY: String = "self"

    private val ACCOUNTS_TTL = 5.minutes
    private val TRANSACTIONS_TTL = 5.minutes

    fun accountsStore(aisp: Aisp, accountDao: AccountDao): Store<String, List<BankAccount>> {
        val validator = DefaultValidator.withTtl<List<BankAccount>>(ACCOUNTS_TTL)
        return StoreFactory.createStore(
            fetcher = Fetcher.of { _ ->
                when (val result = aisp.getAccounts()) {
                    is NetworkResult.Success -> {
                        validator.markFresh()
                        result.data.toBankAccounts()
                    }
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { _ ->
                    accountDao.observeAll().map { rows ->
                        rows.takeIf { it.isNotEmpty() }?.map { it.toBankAccount() }
                    }
                },
                writer = { _, accounts ->
                    accountDao.clear()
                    accountDao.upsertAll(accounts.map { it.toAccountEntity() })
                },
            ),
            validator = validator,
        )
    }

    fun transactionsStore(aisp: Aisp, transactionDao: TransactionDao): Store<String, List<TransactionItem>> {
        val validator = DefaultValidator.withTtl<List<TransactionItem>>(TRANSACTIONS_TTL)
        return StoreFactory.createStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getTransactions(accountId)) {
                    is NetworkResult.Success -> {
                        validator.markFresh()
                        result.data.toTransactionItems(accountId)
                    }
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { accountId ->
                    transactionDao.observeByAccount(accountId).map { rows ->
                        rows.takeIf { it.isNotEmpty() }?.map { it.toTransactionItem() }
                    }
                },
                writer = { accountId, transactions ->
                    transactionDao.clearAccount(accountId)
                    transactionDao.upsertAll(transactions.map { it.toTransactionEntity() })
                },
            ),
            validator = validator,
        )
    }

    fun balancesStore(aisp: Aisp): Store<String, AccountBalance> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getBalances(accountId)) {
                    is NetworkResult.Success -> result.data.toAccountBalance(accountId)
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /** Full account metadata for the account-detail header, keyed by `AccountId`. In-memory. */
    fun accountDetailStore(aisp: Aisp): Store<String, AccountDetail> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getAccountDetails(accountId)) {
                    is NetworkResult.Success ->
                        result.data.toAccountDetail()
                            ?: throw NoSuchElementException("No account detail returned for $accountId")

                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * Every typed balance row for one account, keyed by `AccountId`, in the order the bank returned
     * them. [balancesStore] collapses the same payload to the two figures the home hero card shows.
     * In-memory, as balances are volatile.
     */
    fun balanceLinesStore(aisp: Aisp): Store<String, List<AccountBalanceLine>> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getBalances(accountId)) {
                    is NetworkResult.Success -> result.data.toAccountBalanceLines()
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * Every direct debit mandate for one account, keyed by `AccountId`, with the active/inactive
     * tallies already computed.
     *
     * In-memory with no Room persistence: mandates are read-only reference data behind a consent
     * that can be withdrawn at any moment, so a cached copy could outlive the user's permission to
     * hold it. A miss costs one request, and the OBIE endpoint returns the full list unpaginated.
     */
    fun directDebitsStore(aisp: Aisp): Store<String, DirectDebitsSummary> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getDirectDebits(accountId)) {
                    is NetworkResult.Success -> result.data.toDirectDebitsSummary()
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * Every standing order for one account, keyed by `AccountId`, with the active/inactive tallies
     * and decoded frequency labels already applied.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: standing
     * orders are read-only reference data behind a consent that can be withdrawn at any moment, so
     * a cached copy could outlive the user's permission to hold it. A miss costs one request, and
     * the OBIE endpoint returns the full list unpaginated.
     */
    fun standingOrdersStore(aisp: Aisp): Store<String, StandingOrdersSummary> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getStandingOrders(accountId)) {
                    is NetworkResult.Success -> result.data.toStandingOrdersSummary()
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )
}
