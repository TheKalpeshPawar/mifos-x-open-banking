/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListAction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface routes what it declares
 * (`navigate_consent_detail` from the consent card, `navigate_connect`, `retry_load`,
 * `navigate_reauth`) — the 100% on_click coverage the behaviour gate requires.
 *
 * `navigate_back` is the screen's own `onBack` lambda rather than a routed action.
 */
@OptIn(ExperimentalTestApi::class)
class ConsentListActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** navigate_consent_detail — tapping the consent card carries that consent's id. */
    @Test
    fun tappingTheCardRoutesItsConsentId() = runComposeUiTest {
        var navigatedTo: String? = null
        setContent {
            ConsentListScreenContent(
                state = ConsentListFixtures.contentState(),
                onAction = {},
                onNavigateToDetail = { navigatedTo = it },
                onConnectBank = {},
                onReauthenticate = {},
            )
        }

        onNodeWithTag(ConsentListTestTags.card(ConsentListFixtures.NEAR_EXPIRY_ID)).performClick()

        assertEquals(ConsentListFixtures.NEAR_EXPIRY_ID, navigatedTo)
    }

    /** navigate_connect — the empty state's Connect button raises the host route. */
    @Test
    fun theConnectButtonRaisesTheConnectRoute() = runComposeUiTest {
        var connected = false
        val actions = mutableListOf<ConsentListAction>()
        setContent {
            ConsentListScreenContent(
                state = ConsentListFixtures.emptyState(),
                onAction = { actions.add(it) },
                onNavigateToDetail = {},
                onConnectBank = { connected = true },
                onReauthenticate = {},
            )
        }

        onNodeWithTag(ConsentListTestTags.CONNECT_BUTTON).performClick()

        assertTrue(connected)
        assertTrue(actions.isEmpty())
    }

    /** retry_load — the generic error's Retry dispatches RetryLoad and does not navigate. */
    @Test
    fun theRetryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<ConsentListAction>()
        var reauthenticated = false
        setContent {
            ConsentListScreenContent(
                state = ConsentListFixtures.errorState(),
                onAction = { actions.add(it) },
                onNavigateToDetail = {},
                onConnectBank = {},
                onReauthenticate = { reauthenticated = true },
            )
        }

        onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentListAction>(ConsentListAction.RetryLoad), actions)
        assertTrue(!reauthenticated)
    }

    /** navigate_reauth — the expired-session state raises the sign-in route, not a retry. */
    @Test
    fun theReauthButtonRaisesTheSignInRouteAndDispatchesNoAction() = runComposeUiTest {
        val actions = mutableListOf<ConsentListAction>()
        var reauthenticated = false
        var navigatedTo: String? = null
        setContent {
            ConsentListScreenContent(
                state = ConsentListFixtures.authErrorState(),
                onAction = { actions.add(it) },
                onNavigateToDetail = { navigatedTo = it },
                onConnectBank = {},
                onReauthenticate = { reauthenticated = true },
            )
        }

        onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).performClick()

        assertTrue(reauthenticated)
        assertTrue(actions.isEmpty())
        assertNull(navigatedTo)
    }
}
