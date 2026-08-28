/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.feature.home.components.HomeAccountsSection
import org.mifosx.openbanking.feature.home.components.HomeCardsSection
import org.mifosx.openbanking.feature.home.components.HomeGradientHeader
import org.mifosx.openbanking.feature.home.components.HomeQuickActions
import org.mifosx.openbanking.feature.home.ui.HomeData
import template.core.base.designsystem.theme.KptTheme

/**
 * Content state of the home dashboard: the gradient header, the four quick actions, the customer's
 * cards, and every other account. The quick actions and everything below them are lifted so the card
 * overlaps the header's lower edge.
 */
@Composable
internal fun HomeContent(
    data: HomeData,
    onSendMoney: () -> Unit,
    onSchedule: () -> Unit,
    onStandingOrder: () -> Unit,
    onVrp: () -> Unit,
    onAccountClick: (accountId: String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(HomeTestTags.CONTENT),
    ) {
        HomeGradientHeader(greeting = data.greeting, onAvatarClick = onAvatarClick)
        Column(
            modifier = Modifier
                .offset(y = -KptTheme.spacing.xl)
                .padding(horizontal = KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            HomeQuickActions(
                onSendMoney = onSendMoney,
                onSchedule = onSchedule,
                onStandingOrder = onStandingOrder,
                onVrp = onVrp,
            )
            HomeCardsSection(cards = data.cards, onCardClick = onAccountClick)
            HomeAccountsSection(accounts = data.accounts, onAccountClick = onAccountClick)
        }
    }
}
