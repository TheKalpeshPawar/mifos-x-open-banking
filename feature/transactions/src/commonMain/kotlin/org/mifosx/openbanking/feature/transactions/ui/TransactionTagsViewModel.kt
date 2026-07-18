/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.transactions.CounterpartyNameResolver
import org.mifosx.openbanking.core.data.transactions.TransactionMetadataRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.data.transactions.counterpartyDisplayName
import org.mifosx.openbanking.feature.transactions.formatSigned
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Tags & Notes for one transaction, backed by the real OBP v1.2.1 metadata endpoints.
 * Tag adds/removes apply immediately; the note (newest comment) is saved on Save and the
 * screen then navigates back via [saved]. Transient failures surface through [notice].
 */
class TransactionTagsViewModel(
    private val metadataRepository: TransactionMetadataRepository,
    private val transactionsRepository: TransactionsRepository,
    private val counterpartyNameResolver: CounterpartyNameResolver,
    private val bankId: String,
    private val accountId: String,
    private val transactionId: String,
) : ViewModel() {

    private val state = MutableStateFlow<ScreenState<TransactionTagsContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<TransactionTagsContent>> = state.asStateFlow()

    private val noticeState = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = noticeState.asStateFlow()

    private val savedState = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = savedState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onTagInputChanged(text: String) = updateContent { it.copy(tagInput = text) }

    fun onNoteChanged(text: String) = updateContent { it.copy(note = text) }

    fun onNoticeConsumed() {
        noticeState.value = null
    }

    fun onAddTag() {
        val current = contentOrNull() ?: return
        val normalized = normalizeTag(current.tagInput)
        val problem = when {
            normalized == null -> "Tag cannot be empty. Type a tag name like #groceries."
            current.tags.any { it.value.equals(normalized, ignoreCase = true) } ->
                "That tag is already added to this transaction."
            else -> null
        }
        if (problem != null || normalized == null) {
            noticeState.value = problem
            return
        }
        addTag(normalized)
    }

    /** A suggested-tag chip tapped — values are pre-normalized and absent from the current tags. */
    fun onSuggestionClick(value: String) {
        val current = contentOrNull() ?: return
        if (current.tags.any { it.value.equals(value, ignoreCase = true) }) return
        addTag(value)
    }

    private fun addTag(normalized: String) {
        viewModelScope.launch {
            metadataRepository.addTag(bankId, accountId, transactionId, normalized)
                .onSuccess { tag ->
                    updateContent { it.copy(tags = it.tags + TagChip(tag.id, tag.value), tagInput = "") }
                }
                .onFailure { noticeState.value = "Could not add tag. Please try again." }
        }
    }

    fun onRemoveTag(tagId: String) {
        viewModelScope.launch {
            metadataRepository.removeTag(bankId, accountId, transactionId, tagId)
                .onSuccess { updateContent { it.copy(tags = it.tags.filterNot { tag -> tag.id == tagId }) } }
                .onFailure { noticeState.value = "Could not remove tag. Please try again." }
        }
    }

    fun onSave() {
        val current = contentOrNull() ?: return
        updateContent { it.copy(isSaving = true) }
        viewModelScope.launch {
            metadataRepository.replaceNote(bankId, accountId, transactionId, current.note)
                .onSuccess { savedState.value = true }
                .onFailure {
                    updateContent { it.copy(isSaving = false) }
                    noticeState.value = "Could not save note. Please try again."
                }
        }
    }

    private fun load() {
        state.value = ScreenState.Loading
        viewModelScope.launch {
            val transaction = transactionsRepository.getTransaction(bankId, accountId, transactionId)
                .getOrElse {
                    state.value = ScreenState.Error(it)
                    return@launch
                }
            val metadata = metadataRepository.metadata(bankId, accountId, transactionId)
                .getOrElse {
                    state.value = ScreenState.Error(it)
                    return@launch
                }
            val value = transaction.details.value.amount.toDoubleOrNull() ?: 0.0
            val names = runCatching {
                counterpartyNameResolver.resolve(bankId, accountId, listOf(transaction))
            }.getOrDefault(emptyMap())
            val placeholder = runCatching { counterpartyNameResolver.placeholderHolder() }.getOrDefault("")
            state.value = ScreenState.Content(
                data = TransactionTagsContent(
                    merchant = counterpartyDisplayName(transaction, names, placeholder)
                        .ifBlank { "Transaction" },
                    amount = formatSigned(value, transaction.details.value.currency),
                    isDebit = value < 0,
                    dateTime = formatDateTime(
                        transaction.details.completed.ifBlank { transaction.details.posted },
                    ),
                    tags = metadata.tags.map { TagChip(it.id, it.value) },
                    tagInput = "",
                    note = metadata.note?.value.orEmpty(),
                    isSaving = false,
                    suggestions = emptyList(),
                ).withSuggestions(),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    private fun contentOrNull(): TransactionTagsContent? =
        (state.value as? ScreenState.Content)?.data

    private fun updateContent(transform: (TransactionTagsContent) -> TransactionTagsContent) {
        val current = state.value as? ScreenState.Content ?: return
        state.value = ScreenState.Content(transform(current.data).withSuggestions(), current.freshness)
    }

    /** Recomputes the suggested-tag set: the curated list minus what's already on the transaction. */
    private fun TransactionTagsContent.withSuggestions(): TransactionTagsContent = copy(
        suggestions = PREDEFINED_TAGS.filterNot { predefined ->
            tags.any { it.value.equals(predefined, ignoreCase = true) }
        },
    )

    /** Trims, prefixes "#" when missing; null when nothing remains. */
    private fun normalizeTag(input: String): String? {
        val trimmed = input.trim().trimStart('#').trim()
        if (trimmed.isEmpty()) return null
        return "#$trimmed"
    }
}

/** Display + form state for the Tags & Notes screen. */
@Immutable
data class TransactionTagsContent(
    val merchant: String,
    val amount: String,
    val isDebit: Boolean,
    val dateTime: String,
    val tags: List<TagChip>,
    val tagInput: String,
    val note: String,
    val isSaving: Boolean,
    val suggestions: List<String> = emptyList(),
)

/**
 * Curated spend-category tags offered as one-tap suggestions, most-common-first.
 * Derived from a full taxonomy: housing/bills, food, transport, shopping, health,
 * entertainment, travel, family, money admin, work, and income (credits get tagged too).
 */
internal val PREDEFINED_TAGS = listOf(
    "#groceries", "#rent", "#utilities", "#eating-out", "#coffee", "#takeaway",
    "#fuel", "#public-transport", "#parking", "#shopping", "#clothing", "#gifts",
    "#subscriptions", "#entertainment", "#health", "#gym", "#holiday",
    "#kids", "#pets", "#education", "#insurance", "#savings", "#charity",
    "#work-expense", "#internet", "#salary", "#refund",
)

/** One removable tag chip. */
@Immutable
data class TagChip(
    val id: String,
    val value: String,
)
