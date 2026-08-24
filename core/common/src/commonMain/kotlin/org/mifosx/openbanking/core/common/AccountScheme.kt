/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

/**
 * Which identifier scheme an account's identification is expressed in.
 *
 * Resolved from the OBIE `SchemeName` in the mappers so display and payment formatting key off this
 * enum rather than re-parsing the namespaced string. Lives in `core/common` (not the domain model)
 * because the account formatters here depend on it, and `core/common` must not depend on
 * `core/model`.
 *
 * [Other] is the fallback for a scheme the app does not recognise, including a missing one.
 */
enum class AccountScheme {
    SortCode,
    Iban,
    Pan,
    Other,
    ;

    companion object {
        /** Resolves an OBIE `SchemeName` (e.g. `UK.OBIE.SortCodeAccountNumber`) to its enum. */
        fun fromSchemeName(schemeName: String?): AccountScheme = when {
            schemeName == null -> Other
            schemeName.contains("SortCode", ignoreCase = true) -> SortCode
            schemeName.contains("IBAN", ignoreCase = true) -> Iban
            schemeName.contains("PAN", ignoreCase = true) -> Pan
            else -> Other
        }
    }
}
