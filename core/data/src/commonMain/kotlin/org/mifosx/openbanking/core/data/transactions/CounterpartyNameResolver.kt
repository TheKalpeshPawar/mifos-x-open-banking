/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.transactions

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Transaction

/**
 * Resolves real counterparty names for transactions whose `other_account.holder.name` is the
 * OBP placeholder (the LOGIN USERNAME — stamped whenever the counterparty has no public
 * holder, which covers transfers between the user's own accounts). The placeholder must
 * never reach the UI; consumers substitute the resolved name when present.
 */
interface CounterpartyNameResolver {

    /**
     * Map of `other_account.id` → destination account holder name for the placeholder-holder
     * transactions of the account at ([bankId], [accountId]). Counterparties that cannot be
     * resolved (external merchants, no mirrored booking) are absent from the map.
     */
    suspend fun resolve(bankId: String, accountId: String, transactions: List<Transaction>): Map<String, String>

    /** The placeholder OBP stamps as holder — the login username ("" when unavailable). */
    suspend fun placeholderHolder(): String
}

/**
 * Mirror-join implementation. OBP obfuscates `other_account.id` (stable hash, no lookup
 * endpoint), but a transfer between the user's own accounts books a mirrored transaction on
 * the destination account — same description, same completed timestamp, negated amount. The
 * join identifies the destination account; its holder name comes from the Owner
 * customer-account link (legal name), falling back to the account label. Learned mappings
 * are persisted per viewing account ([ObpCacheDao]) because the obfuscated ids are stable,
 * so each counterparty is joined at most once.
 */
class CounterpartyNameResolverImpl(
    private val accountsRepository: AccountsRepository,
    private val transactionsRepository: TransactionsRepository,
    private val customersRepository: CustomersRepository,
    private val profileRepository: ProfileRepository,
    private val dao: ObpCacheDao,
    private val json: Json,
) : CounterpartyNameResolver {

    override suspend fun resolve(
        bankId: String,
        accountId: String,
        transactions: List<Transaction>,
    ): Map<String, String> {
        val candidates = placeholderCandidates(transactions)
        if (candidates.isEmpty()) return emptyMap()
        val cached = readCache(bankId, accountId)
        val pending = candidates.filterNot { it.otherAccount.id in cached }
        val learned = if (pending.isEmpty()) emptyMap() else mirrorJoin(bankId, accountId, pending)
        val merged = cached + learned
        if (learned.isNotEmpty()) persistCache(bankId, accountId, merged)
        return merged
    }

    override suspend fun placeholderHolder(): String =
        profileRepository.current().getOrNull()?.username.orEmpty()

    /** Transactions carrying the login-username placeholder as holder (blank username → none). */
    private suspend fun placeholderCandidates(transactions: List<Transaction>): List<Transaction> {
        val username = placeholderHolder()
        if (username.isBlank()) return emptyList()
        return transactions.filter { it.otherAccount.holder.name == username && it.otherAccount.id.isNotBlank() }
    }

    private suspend fun mirrorJoin(
        bankId: String,
        accountId: String,
        pending: List<Transaction>,
    ): Map<String, String> {
        val ownAccounts = accountsRepository.myAccounts().getOrNull().orEmpty()
            .filterNot { it.bankId == bankId && it.accountIdOrId == accountId }
            .filter { it.accountIdOrId.isNotBlank() }
        val learned = mutableMapOf<String, String>()
        for (account in ownAccounts) {
            val unresolved = pending.filterNot { it.otherAccount.id in learned }
            if (unresolved.isEmpty()) return learned
            learned += mirroredNames(account, unresolved)
        }
        return learned
    }

    /** Names for the [unresolved] transactions whose mirror books on [account] ("" never returned). */
    private suspend fun mirroredNames(account: Account, unresolved: List<Transaction>): Map<String, String> {
        val mirrors = transactionsRepository.listTransactions(account.bankId, account.accountIdOrId)
            .getOrNull().orEmpty()
        val matched = unresolved.filter { txn -> mirrors.any { isMirror(txn, it) } }
        val name = if (matched.isEmpty()) "" else holderNameOf(account)
        return if (name.isBlank()) emptyMap() else matched.associate { it.otherAccount.id to name }
    }

    private fun isMirror(txn: Transaction, candidate: Transaction): Boolean {
        val amount = txn.details.value.amount.toDoubleOrNull() ?: 0.0
        val candidateAmount = candidate.details.value.amount.toDoubleOrNull() ?: 0.0
        return amount != 0.0 && amount == -candidateAmount &&
            candidate.details.completed == txn.details.completed &&
            candidate.details.description == txn.details.description &&
            candidate.details.value.currency == txn.details.value.currency
    }

    /** Owner customer legal name for [account], falling back to the account label. */
    private suspend fun holderNameOf(account: Account): String =
        customersRepository.accountHolderName(account.bankId, account.accountIdOrId)
            .getOrNull().orEmpty().ifBlank { account.label }

    private suspend fun readCache(bankId: String, accountId: String): Map<String, String> {
        val payload = dao.observe(cacheKey(bankId, accountId)).firstOrNull() ?: return emptyMap()
        return runCatching { json.decodeFromString(namesSerializer, payload) }.getOrDefault(emptyMap())
    }

    private suspend fun persistCache(bankId: String, accountId: String, names: Map<String, String>) {
        dao.upsert(
            ObpCacheEntity(
                storeKey = cacheKey(bankId, accountId),
                payload = json.encodeToString(namesSerializer, names),
            ),
        )
    }

    private fun cacheKey(bankId: String, accountId: String) = "counterparty-names:$bankId/$accountId"

    private val namesSerializer = MapSerializer(String.serializer(), String.serializer())
}
