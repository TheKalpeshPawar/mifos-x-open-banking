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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_avatar_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_detail_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_identity_card_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_label_address
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_label_email
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_label_mobile
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_section_identity
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState

private val SCREEN_PADDING = 16.dp
private val SECTION_GAP = 20.dp
private val CARD_RADIUS = 12.dp
private val AVATAR_SIZE = 88.dp
private val IDENTITY_CARD_PADDING = 24.dp
private val IDENTITY_LINE_GAP = 8.dp
private val ROW_PADDING = 16.dp
private val ROW_ICON_SIZE = 24.dp
private val ROW_ICON_GAP = 16.dp
private val ROW_LINE_GAP = 4.dp

/**
 * The account-holder body: the identity card and the identity detail rows.
 *
 * A plain scrolling column rather than a lazy list — the section count is fixed by the design, so
 * there is nothing here whose length the bank decides. Identity only: consent and sign-out live in
 * Settings → Consents.
 */
@Composable
internal fun AccountHolderContent(
    content: AccountHolderUiState.Content,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(SCREEN_PADDING))
            .testTag(AccountHolderTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
    ) {
        AccountHolderIdentityCard(profile = content.profile)
        AccountHolderIdentitySection(profile = content.profile)
    }
}

/**
 * The avatar, name and role.
 *
 * Flat rather than elevated: it sits directly on the screen background as the page's subject, and a
 * shadow here would rank it against the cards below it that are actually interactive.
 */
@Composable
private fun AccountHolderIdentityCard(profile: PartyProfile, modifier: Modifier = Modifier) {
    val description = stringResource(
        Res.string.feature_account_holder_identity_card_accessibility,
        profile.displayName,
        profile.roleLabel,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(CARD_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountHolderTestTags.IDENTITY_CARD)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(IDENTITY_CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(IDENTITY_LINE_GAP),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountHolderAvatar(initials = profile.initials)
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(AccountHolderTestTags.DISPLAY_NAME),
            )
            Text(
                text = profile.roleLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(AccountHolderTestTags.ROLE_LABEL),
            )
        }
    }
}

@Composable
private fun AccountHolderAvatar(initials: String, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_account_holder_avatar_accessibility, initials)
    Box(
        modifier = modifier
            .size(AVATAR_SIZE)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
            .testTag(AccountHolderTestTags.AVATAR)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Email, mobile and address.
 *
 * Every row hides itself when the bank sent nothing for it — the HSBC UK Personal sandbox never
 * returns `Address`, so that row is normally absent. A labelled row with no value tells the user
 * less than its absence does, and would read as data the app failed to load.
 */
@Composable
private fun AccountHolderIdentitySection(profile: PartyProfile, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHolderSectionHeader(text = stringResource(Res.string.feature_account_holder_section_identity))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(CARD_RADIUS),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AccountHolderTestTags.IDENTITY_SECTION),
        ) {
            Column {
                AccountHolderDetailRow(
                    icon = Icons.Outlined.Email,
                    label = Res.string.feature_account_holder_label_email,
                    value = profile.email,
                    testTag = AccountHolderTestTags.EMAIL_ROW,
                )
                AccountHolderDetailRow(
                    icon = Icons.Outlined.Phone,
                    label = Res.string.feature_account_holder_label_mobile,
                    value = profile.mobile,
                    testTag = AccountHolderTestTags.MOBILE_ROW,
                )
                AccountHolderDetailRow(
                    icon = Icons.Outlined.Home,
                    label = Res.string.feature_account_holder_label_address,
                    value = profile.addressLine,
                    testTag = AccountHolderTestTags.ADDRESS_ROW,
                )
            }
        }
    }
}

@Composable
internal fun AccountHolderSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(bottom = IDENTITY_LINE_GAP)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
    )
}

/** One icon-plus-label-plus-value row, rendered only when [value] carries something. */
@Composable
private fun AccountHolderDetailRow(
    icon: ImageVector,
    label: StringResource,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    if (value.isBlank()) return
    val labelText = stringResource(label)
    val description = stringResource(
        Res.string.feature_account_holder_detail_accessibility,
        labelText,
        value,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ROW_PADDING)
            .testTag(testTag)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
        Column(verticalArrangement = Arrangement.spacedBy(ROW_LINE_GAP)) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
