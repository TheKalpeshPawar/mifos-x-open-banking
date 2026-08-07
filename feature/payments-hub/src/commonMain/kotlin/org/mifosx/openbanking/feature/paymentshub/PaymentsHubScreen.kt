/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubViewModel
import template.core.base.designsystem.theme.KptTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsHubScreen(
    onNavigateToSendMoney: () -> Unit,
    onNavigateToPaymentStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsHubViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mifos Pay",
                        style = KptTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = KptTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* no-op for tab */ }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = null,
                            tint = KptTheme.colorScheme.outline,
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KptTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "UP",
                            style = KptTheme.typography.titleMedium,
                            color = KptTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = KptTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = KptTheme.colorScheme.background,
        modifier = modifier,
    ) { padding ->
        PaymentsHubContent(
            state = state,
            onAction = viewModel::trySendAction,
            onNavigateToSendMoney = onNavigateToSendMoney,
            onNavigateToPaymentStatus = onNavigateToPaymentStatus,
            modifier = Modifier.background(KptTheme.colorScheme.background),
            contentPadding = padding,
        )
    }
}
