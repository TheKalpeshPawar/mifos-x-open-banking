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

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

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

    /** The exact shape HSBC returns on a domestic payment, rendered in a fixed zone. */
    @Test
    fun rendersAnIsoTimestampAsAReadableLocalDateAndTime() {
        assertEquals(
            "5 Aug 2026, 17:46",
            formatDateTime("2026-08-05T17:46:08+00:00", TimeZone.UTC),
        )
        assertEquals(
            "1 Jan 2026, 00:05",
            formatDateTime("2026-01-01T00:05:00Z", TimeZone.UTC),
        )
    }

    /** The offset is honoured rather than ignored — the same instant reads differently by zone. */
    @Test
    fun convertsIntoTheRequestedZone() {
        assertEquals(
            "5 Aug 2026, 23:16",
            formatDateTime("2026-08-05T17:46:08+00:00", TimeZone.of("Asia/Kolkata")),
        )
    }

    /**
     * A bank sending something unexpected must not blank the row or crash the screen. Same contract
     * as [formatShortMonthDay].
     */
    @Test
    fun rendersAnInstantAsAWallClockTime() {
        val instant = Instant.parse("2026-08-05T17:46:08Z")

        assertEquals("17:46", formatTimeOfDay(instant, TimeZone.UTC))
        assertEquals("23:16", formatTimeOfDay(instant, TimeZone.of("Asia/Kolkata")))
    }

    @Test
    fun fallsBackToTheRawInputWhenItWillNotParse() {
        assertEquals("not-a-date", formatDateTime("not-a-date", TimeZone.UTC))
        assertEquals("", formatDateTime("", TimeZone.UTC))
        assertEquals("2026-08-05", formatDateTime("2026-08-05", TimeZone.UTC))
    }
}
