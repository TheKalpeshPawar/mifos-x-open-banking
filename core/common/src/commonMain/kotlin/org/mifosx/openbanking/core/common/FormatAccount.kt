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

private const val IBAN_GROUP = 4

/**
 * Groups an IBAN into space-separated blocks of four, e.g. `"GB29HBUK40051512345678"` →
 * `"GB29 HBUK 4005 1512 3456 78"`. Inputs of four characters or fewer are returned unchanged.
 */
fun formatIban(raw: String): String =
    if (raw.length <= IBAN_GROUP) raw else raw.chunked(IBAN_GROUP).joinToString(" ")

/**
 * Renders an account identification for display, keeping the bank's own value except for an IBAN,
 * which is grouped into four-character blocks for readability. Display-only: the payment mappers send
 * the raw [identification] unchanged.
 */
fun formatAccountIdentifier(scheme: AccountScheme, identification: String): String = when (scheme) {
    AccountScheme.Iban -> formatIban(identification)
    else -> identification
}
