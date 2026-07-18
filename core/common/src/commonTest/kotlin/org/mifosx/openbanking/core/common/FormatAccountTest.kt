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

class FormatAccountTest {

    @Test
    fun groupsSixDigitSortCode() {
        assertEquals("40-05-15", formatSortCode("400515"))
    }

    @Test
    fun returnsInputUnchangedWhenNotSixDigits() {
        assertEquals("4005", formatSortCode("4005"))
        assertEquals("", formatSortCode(""))
    }
}
