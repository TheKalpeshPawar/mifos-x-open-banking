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

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.hsbcPermission.PermissionId
import template.core.base.network.NetworkError

/**
 * Screen state for the profile screen.
 *
 * @property accountId The route argument; the party identity is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class ProfileState(
    val accountId: String,
    val uiState: ProfileUiState = ProfileUiState.Loading,
    /**
     * True while the sign-out confirmation is raised. Screen-level rather than a flag on
     * [ProfileUiState.Content], so sign-out is reachable when the profile failed to load — an
     * expired consent lands the screen in [ProfileUiState.Error], and logout must work from there.
     * The dialog renders over whatever `uiState` is showing.
     */
    val isConfirmingSignOut: Boolean = false,
)

/**
 * The four rendered states.
 *
 * The design draws six surfaces, but two of them — the expiry banner and the sign-out confirmation —
 * are overlays on a rendered profile rather than replacements for it. Modelling them as flags on
 * [Content] rather than sibling members keeps the product of the two representable: a sign-out
 * prompt raised over an expiring consent is one state here, where sealed members would need a
 * seventh case for the combination and a rule for which one wins.
 */
sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    /**
     * A rendered identity.
     *
     * @property profile The party identity. Its fields are blank rather than null when the bank sent
     *   nothing, and each identity row hides itself on a blank value.
     * @property connection The consent behind this session, derived rather than stored.
     * @property permissions Every permission this consent grants, in display order. Hidden behind
     *   [arePermissionsExpanded] because the full OBIE scope runs to twenty-odd entries.
     * @property arePermissionsExpanded True while the granted-permissions list is revealed. Preserved
     *   across stream re-emissions so a refresh cannot collapse a list the user opened.
     * @property isExpiring True when the consent lapses within [EXPIRY_WARNING_DAYS] days; raises
     *   the banner and tints the expiry row.
     * @property daysRemaining Whole days until the consent expires. [Int.MAX_VALUE] when no expiry
     *   was ever stored, which is why [isExpiring] is carried separately rather than recomputed by
     *   the composable from this number.
     */
    data class Content(
        val profile: PartyProfile,
        val connection: ProfileConnectionUi,
        val permissions: List<ProfilePermissionUi>,
        val arePermissionsExpanded: Boolean,
        val isExpiring: Boolean,
        val daysRemaining: Int,
    ) : ProfileUiState

    data object Empty : ProfileUiState

    data class Error(val kind: ProfileErrorKind) : ProfileUiState
}

/**
 * The Open Banking consent behind the current session.
 *
 * @property status Derived from the stored tokens and expiry, never read from a persisted flag.
 * @property expiryLabel The expiry as `26 Sep 2026`. Blank when the consent carried no expiry, and
 *   the row hides itself.
 */
data class ProfileConnectionUi(
    val status: ConsentStatus,
    val expiryLabel: String,
)

/**
 * One granted-permission row.
 *
 * The label is carried here rather than resolved from `composeResources` by [id]: the OBIE scope
 * names live once in [org.mifosx.openbanking.core.model.hsbcPermission.OBPermission], and mirroring
 * all twenty of them into per-id string keys would be a second list to keep in step with the first.
 *
 * @property id The OBIE scope this row reports on, used only for its test tag.
 * @property label The scope's display name, taken from the consent catalogue.
 */
data class ProfilePermissionUi(
    val id: PermissionId,
    val label: String,
)

/**
 * The four failure modes the screen distinguishes, each mapped to its own message.
 *
 * [isRetriable] drives Retry visibility. A consent granted without `ReadParty`, and a bank with no
 * identity record for the account, both fail identically on every attempt — the user has to
 * re-authorise — so offering the button there would be a lie.
 */
enum class ProfileErrorKind(val isRetriable: Boolean) {
    TokenExpired(isRetriable = true),
    ConsentMissingParty(isRetriable = false),
    ProfileNotFound(isRetriable = false),
    LoadFailed(isRetriable = true),
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface ProfileAction {
    data object RetryLoad : ProfileAction

    /** Raises the sign-out confirmation. The profile stays rendered beneath it. */
    data object RequestSignOut : ProfileAction

    /** Dismisses the confirmation without clearing anything. */
    data object DismissSignOut : ProfileAction

    /** Reveals or hides the granted-permissions list. */
    data object TogglePermissions : ProfileAction

    /** Clears the session's tokens and consent from this device. */
    data object ConfirmSignOut : ProfileAction

    data object ManageConsent : ProfileAction

    data object RenewConsent : ProfileAction

    data object Reauthorise : ProfileAction
}

/**
 * One-shot effects the screen turns into navigation.
 *
 * Manage, renew and re-authorise are one destination: all three land the user on consents, which is
 * where a consent is inspected, extended or replaced.
 */
sealed interface ProfileEvent {
    data object NavigateToConsents : ProfileEvent

    /** Sign-out finished: the session is cleared, so the host routes to onboarding. */
    data object LoggedOut : ProfileEvent
}

/**
 * Classifies a stream failure into one of the four [ProfileErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise — a serialization failure, an unmapped status — falls through to
 * [ProfileErrorKind.LoadFailed], which is retriable: an uncategorised fault is more often transient
 * than permanent, and the user is not stranded without a way to try again.
 */
internal fun classifyProfileError(throwable: Throwable): ProfileErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> ProfileErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> ProfileErrorKind.ConsentMissingParty
        is NetworkError.Client.NotFound -> ProfileErrorKind.ProfileNotFound
        else -> ProfileErrorKind.LoadFailed
    }

/** Days-to-expiry at or below which the screen warns the user. */
internal const val EXPIRY_WARNING_DAYS: Int = 7
