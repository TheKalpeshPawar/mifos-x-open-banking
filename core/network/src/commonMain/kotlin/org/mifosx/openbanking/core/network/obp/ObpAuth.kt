/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.obp

/**
 * Builders for the two OBP DirectLogin `Authorization` header shapes.
 *
 * - login: `DirectLogin username="u", password="p", consumer_key="k"`
 * - authed calls: `DirectLogin token="t"`
 */
object ObpAuth {

    fun loginHeader(username: String, password: String, consumerKey: String): String =
        "DirectLogin username=\"$username\", password=\"$password\", consumer_key=\"$consumerKey\""

    fun tokenHeader(token: String): String = "DirectLogin token=\"$token\""
}
