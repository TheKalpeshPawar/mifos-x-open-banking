/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

/**
 * Stable `testTag` values for the beneficiaries screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 */
internal object BeneficiariesTestTags {

    const val SEARCH_FIELD = "beneficiaries:searchField"

    const val CONTENT_LIST = "beneficiaries:contentList"

    /** Tag for one payee row, keyed by its OBIE `BeneficiaryId`. */
    fun row(beneficiaryId: String): String = "beneficiaries:row:$beneficiaryId"
}
