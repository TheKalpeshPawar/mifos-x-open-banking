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
import org.mifosx.openbanking.core.data.banking.AccountCapabilityRegistry
import org.mifosx.openbanking.core.data.banking.mapper.toAccountBalance
import org.mifosx.openbanking.core.data.banking.mapper.toAccountBalanceLines
import org.mifosx.openbanking.core.data.banking.mapper.toAccountDetail
import org.mifosx.openbanking.core.data.banking.mapper.toAccountEntity
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccount
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccounts
import org.mifosx.openbanking.core.data.banking.mapper.toDirectDebitsSummary
import org.mifosx.openbanking.core.data.banking.mapper.toPartyProfile
import org.mifosx.openbanking.core.data.banking.mapper.toScheduledPaymentItems
import org.mifosx.openbanking.core.data.banking.mapper.toStandingOrdersSummary
import org.mifosx.openbanking.core.data.banking.mapper.toStatementDetail
import org.mifosx.openbanking.core.data.banking.mapper.toStatementPeriods
import org.mifosx.openbanking.core.data.banking.mapper.toStatementTransactionItems
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionDetails
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionEntity
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItem
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItems
import org.mifosx.openbanking.core.data.util.isUnsupportedForProduct
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.database.banking.dao.AccountDao
import org.mifosx.openbanking.core.database.banking.dao.TransactionDao
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.network.api.Aisp
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkError
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

    /**
     * Every rich transaction record for one account, keyed by `AccountId`, as the transaction-detail
     * screen resolves them client-side by `transactionId`.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: transaction
     * detail is read-only reference data behind a consent that can be withdrawn at any moment, so a
     * cached copy could outlive the user's permission to hold it. A miss costs one request, and the
     * OBIE transactions endpoint returns the list unpaginated for the detail lookup.
     *
     * Takes no capability registry: the transactions endpoint is supported for every HSBC product, so
     * a refusal is not an unsupported-feature signal and surfaces as an ordinary error.
     */
    fun transactionDetailsStore(aisp: Aisp): Store<String, List<TransactionDetail>> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getTransactions(accountId)) {
                    is NetworkResult.Success -> result.data.toTransactionDetails(accountId)
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

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
    fun directDebitsStore(
        aisp: Aisp,
        capabilityRegistry: AccountCapabilityRegistry,
    ): Store<String, DirectDebitsSummary> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getDirectDebits(accountId)) {
                    is NetworkResult.Success -> result.data.toDirectDebitsSummary()
                    is NetworkResult.Error -> {
                        result.error.recordIfUnsupported(
                            accountId = accountId,
                            endpoint = AccountEndpoint.DirectDebits,
                            registry = capabilityRegistry,
                        )
                        throw result.error.toThrowable()
                    }
                }
            },
        )

    /**
     * The connected party's identity for one account, keyed by `AccountId`.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: identity is
     * read-only reference data behind a consent that can be withdrawn at any moment, and it is the
     * most personal payload the app holds — a cached copy outliving the consent is exactly what
     * should not happen. A miss costs one request.
     *
     * Takes no capability registry: there is no `AccountEndpoint.Party`, so a `U000` refusal is not
     * recorded and the screen surfaces it as an ordinary error.
     */
    fun partyStore(aisp: Aisp): Store<String, PartyProfile> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getParty(accountId)) {
                    is NetworkResult.Success -> result.data.toPartyProfile()
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * Every statement period for one account, keyed by `AccountId`, sorted newest-first.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: statements are
     * read-only reference data behind a consent that can be withdrawn at any moment, so a cached copy
     * could outlive the user's permission to hold it. A miss costs one request, and the OBIE endpoint
     * returns the full list unpaginated.
     */
    fun statementsStore(aisp: Aisp): Store<String, List<StatementPeriod>> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getStatements(accountId)) {
                    is NetworkResult.Success -> result.data.toStatementPeriods()
                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * One statement's identity, period and money lines, keyed by a composite `"$accountId|$statementId"`
     * so the fetcher can reach both path segments the OBIE call needs.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: statement detail
     * is read-only reference data behind a consent that can be withdrawn at any moment. An empty
     * `Statement` array is a not-found — the mapper returns `null` and the fetcher throws, surfacing it
     * as an error rather than a blank screen.
     *
     * Takes no capability registry: statement detail sits behind the already-gated Statements chip, and
     * the detail/transactions endpoints do not answer `U000`, so a refusal is an ordinary error.
     */
    fun statementDetailStore(aisp: Aisp): Store<String, StatementDetail> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { key ->
                val (accountId, statementId) = key.split("|", limit = 2)
                when (val result = aisp.getStatementDetails(accountId, statementId)) {
                    is NetworkResult.Success ->
                        result.data.toStatementDetail(accountId, statementId)
                            ?: throw NoSuchElementException("No statement $statementId for account $accountId")

                    is NetworkResult.Error -> throw result.error.toThrowable()
                }
            },
        )

    /**
     * Every transaction booked within one statement's period, keyed by the same composite
     * `"$accountId|$statementId"` the detail store uses. In-memory, for the same consent-scope reason as
     * [statementDetailStore].
     */
    fun statementTransactionsStore(aisp: Aisp): Store<String, List<TransactionItem>> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { key ->
                val (accountId, statementId) = key.split("|", limit = 2)
                when (val result = aisp.getStatementTransactions(accountId, statementId)) {
                    is NetworkResult.Success -> result.data.toStatementTransactionItems(accountId)
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
    fun standingOrdersStore(
        aisp: Aisp,
        capabilityRegistry: AccountCapabilityRegistry,
    ): Store<String, StandingOrdersSummary> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getStandingOrders(accountId)) {
                    is NetworkResult.Success -> result.data.toStandingOrdersSummary()
                    is NetworkResult.Error -> {
                        result.error.recordIfUnsupported(
                            accountId = accountId,
                            endpoint = AccountEndpoint.StandingOrders,
                            registry = capabilityRegistry,
                        )
                        throw result.error.toThrowable()
                    }
                }
            },
        )

    /**
     * Every future-dated scheduled payment for one account, keyed by `AccountId`, in the order the
     * bank returned them.
     *
     * In-memory with no Room persistence, for the same reason as [directDebitsStore]: scheduled
     * payments are read-only reference data behind a consent that can be withdrawn at any moment, so
     * a cached copy could outlive the user's permission to hold it. A miss costs one request, and the
     * OBIE endpoint returns the full list unpaginated.
     *
     * Product-gated like [directDebitsStore] and [standingOrdersStore]: scheduled payments are not
     * offered on a credit card, so a `U000` refusal is recorded against
     * [AccountEndpoint.ScheduledPayments] before rethrowing, letting account-detail hide the chip.
     */
    fun scheduledPaymentsStore(
        aisp: Aisp,
        capabilityRegistry: AccountCapabilityRegistry,
    ): Store<String, List<ScheduledPaymentItem>> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { accountId ->
                when (val result = aisp.getScheduledPayments(accountId)) {
                    is NetworkResult.Success -> result.data.toScheduledPaymentItems(accountId)
                    is NetworkResult.Error -> {
                        result.error.recordIfUnsupported(
                            accountId = accountId,
                            endpoint = AccountEndpoint.ScheduledPayments,
                            registry = capabilityRegistry,
                        )
                        throw result.error.toThrowable()
                    }
                }
            },
        )

    /**
     * Notes a `U000` refusal so the account-detail screen stops offering the feature.
     *
     * Recorded in the fetcher rather than a ViewModel because this is the one point every call to
     * these endpoints passes through — including a deep link that bypasses the chip entirely — and
     * because the record has to outlive any single screen.
     */
    private fun NetworkError.recordIfUnsupported(
        accountId: String,
        endpoint: AccountEndpoint,
        registry: AccountCapabilityRegistry,
    ) {
        if (isUnsupportedForProduct()) registry.markUnsupported(accountId, endpoint)
    }
}
