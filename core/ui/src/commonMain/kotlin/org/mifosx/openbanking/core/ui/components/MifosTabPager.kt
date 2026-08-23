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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A tab row over a horizontal pager, one page per tab. */
@Composable
fun MifosTabPager(
    pagerState: PagerState,
    currentPage: Int,
    tabs: List<String>,
    setCurrentPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    Column(modifier = modifier) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[currentPage])
                        .padding(start = KptTheme.spacing.md, end = KptTheme.spacing.md),
                )
            },
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                Tab(
                    modifier = Modifier.padding(all = KptTheme.spacing.md),
                    selected = currentPage == index,
                    onClick = { setCurrentPage(index) },
                ) {
                    Text(text = tabTitle)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageContent = { page ->
                content(page)
            },
        )
    }
}

@Preview
@Composable
private fun MifosTabPagerPreview() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    MifosXOpenBankingTheme {
        MifosTabPager(
            pagerState = pagerState,
            currentPage = 0,
            tabs = listOf("Tab 1", "Tab 2"),
            setCurrentPage = {},
        ) { page ->
            Text(text = "Page $page")
        }
    }
}
