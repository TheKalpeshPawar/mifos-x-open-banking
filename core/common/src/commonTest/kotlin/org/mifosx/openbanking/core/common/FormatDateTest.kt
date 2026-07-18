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

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDateTest {

    @Test
    fun rendersShortDayAndMonthFromIsoTimestamp() {
        assertEquals("27 Jun", formatShortMonthDay("2026-06-27T10:00:00Z"))
        assertEquals("1 Jan", formatShortMonthDay("2026-01-01T00:00:00+00:00"))
    }

    @Test
    fun fallsBackForNonIsoInput() {
        assertEquals("not-a-date", formatShortMonthDay("not-a-date"))
        assertEquals("2026/06", formatShortMonthDay("2026/06"))
    }
}
