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
    fun groupsIbanIntoBlocksOfFour() {
        assertEquals("GB29 HBUK 4005 1512 3456 78", formatIban("GB29HBUK40051512345678"))
    }

    @Test
    fun returnsIbanUnchangedWhenFourOrFewerChars() {
        assertEquals("GB29", formatIban("GB29"))
        assertEquals("GB", formatIban("GB"))
    }

    @Test
    fun formatsAccountIdentifierPerScheme() {
        assertEquals("40051512345678", formatAccountIdentifier(AccountScheme.SortCode, "40051512345678"))
        assertEquals(
            "GB29 HBUK 4005 1512 3456 78",
            formatAccountIdentifier(AccountScheme.Iban, "GB29HBUK40051512345678"),
        )
        assertEquals("xxxx-xxxx-xxxx-3456", formatAccountIdentifier(AccountScheme.Pan, "xxxx-xxxx-xxxx-3456"))
        assertEquals("raw-fallback", formatAccountIdentifier(AccountScheme.Other, "raw-fallback"))
    }
}
