/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.account

import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_credit
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_current
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_money
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_other
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_savings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [accountTypeLabelRes] — the single mapping from an OBIE `AccountTypeCode` plus `Description`
 * to a localized product label. Non-composable, so it is asserted without a Compose runtime.
 */
class AccountDisplayNameTest {

    @Test
    fun aGlobalMoneyWalletResolvesToItsOwnLabelDespiteReportingCacc() {
        assertEquals(
            Res.string.core_ui_account_type_global_money,
            accountTypeLabelRes(accountTypeCode = "CACC", description = "GLOBAL MONEY ACCOUNT"),
        )
    }

    @Test
    fun aPlainCurrentAccountIsUnaffectedByTheGlobalMoneyMarker() {
        assertEquals(
            Res.string.core_ui_account_type_current,
            accountTypeLabelRes(accountTypeCode = "CACC", description = "Description of the account"),
        )
    }

    @Test
    fun theRemainingProductCodesResolveToTheirLabels() {
        assertEquals(
            Res.string.core_ui_account_type_savings,
            accountTypeLabelRes(accountTypeCode = "SVGS", description = ""),
        )
        assertEquals(
            Res.string.core_ui_account_type_credit,
            accountTypeLabelRes(accountTypeCode = "CARD", description = ""),
        )
        assertEquals(
            Res.string.core_ui_account_type_other,
            accountTypeLabelRes(accountTypeCode = "XXXX", description = ""),
        )
    }

    @Test
    fun aCardCodeStillResolvesToTheCreditLabelWithoutThePanScheme() {
        assertEquals(
            Res.string.core_ui_account_type_credit,
            accountTypeLabelRes(accountTypeCode = "ccrd", description = ""),
        )
    }
}
