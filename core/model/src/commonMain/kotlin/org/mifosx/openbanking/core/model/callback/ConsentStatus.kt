/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.callback

enum class ConsentStatus {
    AwaitingAuthorisation,
    Authorised,
    Rejected,
    Revoked,
    Expired,
    Consumed,
    ;

    companion object {
        fun fromString(raw: String): ConsentStatus = when (raw) {
            "AWAU", "AwaitingAuthorisation" -> AwaitingAuthorisation
            "AUTH", "Authorised" -> Authorised
            "Rejected" -> Rejected
            "Revoked" -> Revoked
            "Expired" -> Expired
            "Consumed" -> Consumed
            else -> Rejected
        }
    }
}
