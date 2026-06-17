/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import org.mifosx.openbanking.core.model.obp.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileDerivationTest {

    @Test
    fun initialsOf_takesUpToTwo() {
        assertEquals("MS", initialsOf("Maria Santos"))
        assertEquals("A", initialsOf("Amina"))
        assertEquals("", initialsOf("   "))
        assertEquals("AW", initialsOf("amina  wanjiru  ke"))
    }

    @Test
    fun toContent_seedsFullNameFromUsernameAndInitials() {
        val content = UserProfile(username = "Maria Santos", email = "maria@example.com").toContent()
        assertEquals("Maria Santos", content.displayName)
        assertEquals("Maria Santos", content.fullName)
        assertEquals("MS", content.initials)
        assertEquals("maria@example.com", content.email)
    }

    @Test
    fun toContent_fallsBackToEmailLocalPartWhenUsernameBlank() {
        val content = UserProfile(username = "", email = "amina.wanjiru@gmail.com").toContent()
        assertEquals("amina.wanjiru", content.displayName)
        assertEquals("A", content.initials)
    }
}
