/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_loading_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_loading_caption

private val ContentPadding = 24.dp
private val LineGap = 12.dp

/**
 * Loading state: a spinner with a caption naming what is being fetched.
 *
 * A spinner rather than a shaped skeleton, unlike the list screens. The profile's own layout depends
 * on which identity rows the bank fills in, so a placeholder would have to guess a shape it cannot
 * know — and guessing wrong makes the arrival jump further than a spinner does.
 */
@Composable
internal fun AccountHolderSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_account_holder_loading_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ContentPadding)
            .testTag(AccountHolderTestTags.LOADING)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(Res.string.feature_account_holder_loading_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(AccountHolderTestTags.LOADING_CAPTION),
        )
    }
}
