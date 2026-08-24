/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.mifosx.openbanking.core.data.banking.BeneficiariesRepository
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the beneficiaries list: one row per saved payee for the account, with a client-side search.
 *
 * Navigation is not modelled as an action — the screen owns back and the consent route through its
 * lambdas, matching scheduled-payments — so this stays a pure state machine over the payee stream,
 * with no one-shot events (`Event = Nothing`).
 *
 * Search never touches the network. The unfiltered list is held on the state so a query can be
 * re-applied or cleared without a re-fetch, and a query that matches nothing stays in `Content` with
 * an empty filtered list rather than falling into `Empty` — which would wrongly claim the account
 * has no payees at all.
 */
class BeneficiariesViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BeneficiariesRepository,
) : BaseViewModel<BeneficiariesState, Nothing, BeneficiariesAction>(
    initialState = BeneficiariesState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.beneficiariesStream(state.accountId, viewModelScope)

    init {
        stream.state
            .emptyIfContent { beneficiaries -> beneficiaries.isEmpty() }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState(query())) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: BeneficiariesAction) {
        when (action) {
            BeneficiariesAction.RetryLoad -> stream.refresh()
            is BeneficiariesAction.Search -> applySearch(action.query)
        }
    }

    /**
     * The live query, so a stream re-emission (a refresh landing, say) re-applies the filter the
     * user still has typed instead of silently resetting the list under them.
     */
    private fun query(): String = (state.uiState as? BeneficiariesUiState.Content)?.query.orEmpty()

    private fun applySearch(query: String) {
        val current = state.uiState as? BeneficiariesUiState.Content ?: return
        updateState {
            copy(uiState = current.copy(query = query, filtered = current.all.matching(query)))
        }
    }

    private fun ScreenState<List<BeneficiaryItem>>.toUiState(query: String): BeneficiariesUiState =
        when (this) {
            is ScreenState.Content -> {
                val rows = data
                BeneficiariesUiState.Content(all = rows, filtered = rows.matching(query), query = query)
            }

            is ScreenState.Error -> BeneficiariesUiState.Error(classifyBeneficiariesError(error))
            is ScreenState.NoNetwork -> BeneficiariesUiState.Error(BeneficiariesErrorKind.NetworkError)
            ScreenState.Unauthenticated -> BeneficiariesUiState.Error(BeneficiariesErrorKind.TokenExpired)
            ScreenState.Empty -> BeneficiariesUiState.Empty
            ScreenState.Loading -> BeneficiariesUiState.Loading
        }

    companion object {
        /** Must match the [BeneficiariesRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

private const val IBAN_GROUP_SIZE = 4

/**
 * Filters payees by creditor name or payment reference, case-insensitively.
 *
 * Only those two fields are searched — matching on the account identifier would let a stray digit
 * surface an unrelated payee. A blank query matches everything, which is what makes clearing the
 * field restore the whole list.
 */
internal fun List<BeneficiaryItem>.matching(query: String): List<BeneficiaryItem> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this
    return filter {
        it.creditorName.contains(trimmed, ignoreCase = true) || it.reference.contains(trimmed, ignoreCase = true)
    }
}

/**
 * Groups an IBAN into four-character blocks for display, leaving every other scheme untouched.
 *
 * Sort codes and card numbers already arrive with their own separators from the bank; re-spacing
 * them would fight the format the user recognises. An IBAN arrives unspaced and is unreadable that
 * way, so it is the only scheme that gets grouped.
 */
internal fun formatIdentification(identification: String, scheme: BeneficiaryScheme): String =
    if (scheme == BeneficiaryScheme.Iban) {
        identification.filterNot { it.isWhitespace() }.chunked(IBAN_GROUP_SIZE).joinToString(separator = " ")
    } else {
        identification
    }
