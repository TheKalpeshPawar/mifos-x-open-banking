/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.mifosx.openbanking.core.data.banking.ProfileRepository
import org.mifosx.openbanking.core.model.banking.PartyProfile
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the account-holder screen: the party identity behind the account.
 *
 * The screen is reached from the account-detail "Account holder" chip, so [ACCOUNT_ID_ARG] is always
 * a valid account id — the party stream loads the holder for that account. It shows identity only;
 * consent management and sign-out live in Settings → Consents, not here, so the screen owns no
 * navigation and its event type is [Nothing].
 */
class AccountHolderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
) : BaseViewModel<AccountHolderState, Nothing, AccountHolderAction>(
    initialState = AccountHolderState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.profileStream(state.accountId, viewModelScope)

    init {
        stream.state
            .emptyIfContent { profile -> profile.isEmpty }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AccountHolderAction) {
        when (action) {
            AccountHolderAction.RetryLoad -> stream.refresh()
        }
    }

    private fun ScreenState<PartyProfile>.toUiState(): AccountHolderUiState = when (this) {
        is ScreenState.Content -> data.toContent()
        is ScreenState.Error -> AccountHolderUiState.Error(classifyAccountHolderError(error))
        is ScreenState.NoNetwork -> AccountHolderUiState.Error(AccountHolderErrorKind.LoadFailed)
        ScreenState.Unauthenticated -> AccountHolderUiState.Error(AccountHolderErrorKind.TokenExpired)
        ScreenState.Empty -> AccountHolderUiState.Empty
        ScreenState.Loading -> AccountHolderUiState.Loading
    }

    /**
     * A payload with no name renders the empty state even though the stream reported Content — the
     * bank answering without an identity is a real answer, not missing data.
     */
    private fun PartyProfile.toContent(): AccountHolderUiState = if (isEmpty) {
        AccountHolderUiState.Empty
    } else {
        AccountHolderUiState.Content(profile = this)
    }

    companion object {
        /** Must match the [AccountHolderRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}
