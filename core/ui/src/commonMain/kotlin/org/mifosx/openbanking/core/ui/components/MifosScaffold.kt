/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosScaffold(
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarTitle: String? = null,
    containerColor: Color = KptTheme.colorScheme.surface,
    contentColor: Color = KptTheme.colorScheme.onSurface,
    floatingActionButtonContent: FloatingActionButtonContent? = null,
    pullToRefreshState: MifosPullToRefreshState = rememberMifosPullToRefreshState(),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            if (topBarTitle != null) {
                MifosTopBar(
                    topBarTitle = topBarTitle,
                    onNavigationIconClick = onNavigationIconClick,
                    actions = actions,
                )
            }
        },
        floatingActionButton = {
            floatingActionButtonContent?.let { fab ->
                FloatingActionButton(
                    onClick = fab.onClick,
                    contentColor = fab.contentColor,
                    content = fab.content,
                )
            }
        },
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = WindowInsets(0.dp),
        content = { paddingValues ->
            val internalPullToRefreshState = rememberPullToRefreshState()
            Box(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .consumeWindowInsets(paddingValues = paddingValues)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(insets = contentWindowInsets)
                        .pullToRefresh(
                            state = internalPullToRefreshState,
                            isRefreshing = pullToRefreshState.isRefreshing,
                            onRefresh = pullToRefreshState.onRefresh,
                            enabled = pullToRefreshState.isEnabled,
                        ),
                ) {
                    Column(modifier = Modifier) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = KptTheme.colorScheme.outlineVariant,
                        )
                        content()
                    }

                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = pullToRefreshState.isRefreshing,
                        state = internalPullToRefreshState,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosScaffold(
    showNavigationIcon: Boolean,
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit = {},
    topBarTitle: String? = null,
    containerColor: Color = KptTheme.colorScheme.surface,
    contentColor: Color = KptTheme.colorScheme.onSurface,
    floatingActionButtonContent: FloatingActionButtonContent? = null,
    pullToRefreshState: MifosPullToRefreshState = rememberMifosPullToRefreshState(),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            if (topBarTitle != null) {
                MifosTopBar(
                    topBarTitle = topBarTitle,
                    showNavigationIcon = showNavigationIcon,
                    onNavigationIconClick = onNavigationIconClick,
                    actions = actions,
                )
            }
        },
        floatingActionButton = {
            floatingActionButtonContent?.let { fab ->
                FloatingActionButton(
                    onClick = fab.onClick,
                    contentColor = fab.contentColor,
                    content = fab.content,
                )
            }
        },
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = WindowInsets(0.dp),
        content = { paddingValues ->
            val internalPullToRefreshState = rememberPullToRefreshState()
            Box(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .consumeWindowInsets(paddingValues = paddingValues)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(insets = contentWindowInsets)
                        .pullToRefresh(
                            state = internalPullToRefreshState,
                            isRefreshing = pullToRefreshState.isRefreshing,
                            onRefresh = pullToRefreshState.onRefresh,
                            enabled = pullToRefreshState.isEnabled,
                        ),
                ) {
                    Column(modifier = Modifier) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = KptTheme.colorScheme.outlineVariant,
                        )
                        content()
                    }

                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = pullToRefreshState.isRefreshing,
                        state = internalPullToRefreshState,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosElevatedScaffold(
    onNavigateBack: () -> Unit,
    topBarTitle: String,
    modifier: Modifier = Modifier,
    brandIcon: ImageVector? = null,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = KptTheme.colorScheme.surface,
    contentColor: Color = KptTheme.colorScheme.onSurface,
    floatingActionButtonContent: FloatingActionButtonContent? = null,
    pullToRefreshState: MifosPullToRefreshState = rememberMifosPullToRefreshState(),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            MifosRoundedTopAppBar(
                brandIcon = brandIcon,
                title = topBarTitle,
                onNavigateBack = onNavigateBack,
                actions = actions,
            )
        },
        floatingActionButton = {
            floatingActionButtonContent?.let { fab ->
                FloatingActionButton(
                    onClick = fab.onClick,
                    contentColor = fab.contentColor,
                    content = fab.content,
                )
            }
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .consumeWindowInsets(paddingValues = paddingValues)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                val internalPullToRefreshState = rememberPullToRefreshState()
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(insets = contentWindowInsets)
                        .pullToRefresh(
                            state = internalPullToRefreshState,
                            isRefreshing = pullToRefreshState.isRefreshing,
                            onRefresh = pullToRefreshState.onRefresh,
                            enabled = pullToRefreshState.isEnabled,
                        ),
                ) {
                    content()

                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = pullToRefreshState.isRefreshing,
                        state = internalPullToRefreshState,
                        containerColor = KptTheme.colorScheme.tertiary,
                        color = KptTheme.colorScheme.primary,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    pullToRefreshState: MifosPullToRefreshState = rememberMifosPullToRefreshState(),
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = KptTheme.colorScheme.surface,
    contentColor: Color = KptTheme.colorScheme.onSurface,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                floatingActionButton()
            }
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .consumeWindowInsets(paddingValues = paddingValues)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                val internalPullToRefreshState = rememberPullToRefreshState()
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(insets = contentWindowInsets)
                        .pullToRefresh(
                            state = internalPullToRefreshState,
                            isRefreshing = pullToRefreshState.isRefreshing,
                            onRefresh = pullToRefreshState.onRefresh,
                            enabled = pullToRefreshState.isEnabled,
                        ),
                ) {
                    content()

                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = pullToRefreshState.isRefreshing,
                        state = internalPullToRefreshState,
                        containerColor = KptTheme.colorScheme.tertiary,
                        color = KptTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

data class FloatingActionButtonContent(
    val onClick: (() -> Unit),
    val contentColor: Color,
    val content: (@Composable () -> Unit),
)

data class MifosPullToRefreshState(
    val isEnabled: Boolean,
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
)

@Composable
fun rememberMifosPullToRefreshState(
    isEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
): MifosPullToRefreshState = remember(isEnabled, isRefreshing, onRefresh) {
    MifosPullToRefreshState(
        isEnabled = isEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    )
}

@Preview
@Composable
private fun MifosElevatedScaffoldPreview() {
    MifosXOpenBankingTheme {
        MifosElevatedScaffold(onNavigateBack = {}, topBarTitle = "Mifos")
    }
}
