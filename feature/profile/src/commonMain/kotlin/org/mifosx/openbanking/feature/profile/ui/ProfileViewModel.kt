/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.data.banking.ProfileRepository
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Drives the profile screen: the party identity, the consent behind it, and sign-out.
 *
 * The identity comes from the party stream; the consent state does not. Consent status and expiry
 * are **derived** from [ConsentSession] on every emission rather than read from a persisted field,
 * for the same reason `isActive()` derives sign-in from the tokens: a stored status can claim a
 * consent is authorised after it has lapsed, and a screen whose whole purpose is to report on the
 * consent is the worst place for that to be wrong.
 *
 * All display formatting happens here. The composables receive finished strings, which keeps date
 * rendering testable on the JVM without a Compose runtime.
 */
@OptIn(ExperimentalTime::class)
class ProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val consentSession: ConsentSession,
) : BaseViewModel<ProfileState, ProfileEvent, ProfileAction>(
    initialState = ProfileState(
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

    override fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.RetryLoad -> stream.refresh()
            ProfileAction.RequestSignOut -> setConfirmingSignOut(true)
            ProfileAction.DismissSignOut -> setConfirmingSignOut(false)
            ProfileAction.ConfirmSignOut -> signOut()
            ProfileAction.ManageConsent,
            ProfileAction.RenewConsent,
            ProfileAction.Reauthorise,
            -> sendEvent(ProfileEvent.NavigateToConsents)
        }
    }

    /**
     * Toggles the confirmation flag without touching the rendered profile.
     *
     * A no-op outside [ProfileUiState.Content]: there is nothing to sign out of from a skeleton or
     * an error, and forcing a Content state to carry the flag would invent an identity the bank
     * never sent.
     */
    private fun setConfirmingSignOut(confirming: Boolean) {
        val current = state.uiState
        if (current !is ProfileUiState.Content) return
        updateState { copy(uiState = current.copy(isConfirmingSignOut = confirming)) }
    }

    /**
     * Clears the tokens and consent, then lowers the dialog.
     *
     * The screen is not navigated away from here — the host observes the session and moves the user
     * once it goes inactive, so a feature that never sees the route table stays that way.
     */
    private fun signOut() {
        consentSession.clear()
        setConfirmingSignOut(false)
    }

    private fun ScreenState<PartyProfile>.toUiState(): ProfileUiState = when (this) {
        is ScreenState.Content -> data.toContent()
        is ScreenState.Error -> ProfileUiState.Error(classifyProfileError(error))
        is ScreenState.NoNetwork -> ProfileUiState.Error(ProfileErrorKind.LoadFailed)
        ScreenState.Unauthenticated -> ProfileUiState.Error(ProfileErrorKind.TokenExpired)
        ScreenState.Empty -> ProfileUiState.Empty
        ScreenState.Loading -> ProfileUiState.Loading
    }

    /**
     * A payload with no name renders the empty state even though the stream reported Content — the
     * bank answering without an identity is a real answer, not missing data.
     */
    private fun PartyProfile.toContent(): ProfileUiState = if (isEmpty) {
        ProfileUiState.Empty
    } else {
        val expiry = consentSession.consentExpiration()
        val days = consentDaysRemaining(expiry)
        ProfileUiState.Content(
            profile = this,
            connection = ProfileConnectionUi(
                status = consentStatus(expiry),
                expiryLabel = expiry?.let(::formatConsentExpiry).orEmpty(),
            ),
            permissions = profilePermissions(),
            isExpiring = days <= EXPIRY_WARNING_DAYS,
            daysRemaining = days,
            isConfirmingSignOut = isConfirmingSignOut(),
        )
    }

    /** Preserves a raised dialog across a stream re-emission, so a refresh cannot dismiss it. */
    private fun isConfirmingSignOut(): Boolean =
        (state.uiState as? ProfileUiState.Content)?.isConfirmingSignOut == true

    /**
     * Resolves the consent's status from the session rather than a stored label.
     *
     * No stored expiry reads as [ConsentStatus.Authorised]: HSBC does not always return one, and
     * treating its absence as expiry would sign a working consent out on sight.
     */
    private fun consentStatus(expiry: Instant?): ConsentStatus = when {
        !consentSession.isActive() -> ConsentStatus.Revoked
        expiry != null && expiry < Clock.System.now() -> ConsentStatus.Expired
        else -> ConsentStatus.Authorised
    }

    /**
     * Whole days until the consent lapses, or [Int.MAX_VALUE] when none was stored.
     *
     * The sentinel is what keeps a consent with no recorded expiry out of the warning banner, and
     * mirrors `AccountsViewModel.consentDaysRemaining` so the two surfaces cannot disagree about
     * whether the same session is close to lapsing.
     */
    private fun consentDaysRemaining(expiry: Instant?): Int =
        expiry?.let { (it - Clock.System.now()).inWholeDays.toInt() } ?: Int.MAX_VALUE

    companion object {
        /** Must match the [ProfileRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

/**
 * Formats a consent expiry as `26 Sep 2026`, the form the connection card shows.
 *
 * The day is zero-padded because this is a fixed-width metadata row rather than prose, and a column
 * of dates that shifts by a character between the 9th and the 10th reads as misaligned.
 */
@OptIn(ExperimentalTime::class)
internal fun formatConsentExpiry(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val day = date.day.toString().padStart(DAY_WIDTH, '0')
    val month = MONTH_ABBREVIATIONS[date.month.ordinal]
    return "$day $month ${date.year}"
}

private const val DAY_WIDTH = 2
private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
