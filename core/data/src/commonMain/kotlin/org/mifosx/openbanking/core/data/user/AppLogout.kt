/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.user

/**
 * Logs the PSU out completely: revokes the consent best-effort, forgets the session, and clears all
 * local data.
 *
 * The single logout path behind both the profile "Sign out" and the consent-detail "Revoke access".
 * It always completes — an expired, revoked, or unreachable consent does not block the local
 * teardown — and leaves the app in a state the root navigator reads as signed-out, so it routes back
 * to onboarding on its own.
 */
interface AppLogout {

    /**
     * Runs the full logout sequence. Safe to call from any state: a missing or already-dead consent
     * is not an error, it just skips the revoke.
     */
    suspend fun logOut()
}
