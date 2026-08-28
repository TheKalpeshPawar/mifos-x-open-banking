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

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
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
import template.core.base.designsystem.theme.KptTheme

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
            .padding(PaddingValues(KptTheme.spacing.md))
            .testTag(AccountHolderTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.lg),
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
        color = KptTheme.colorScheme.surfaceContainerHigh,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountHolderTestTags.IDENTITY_CARD)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountHolderAvatar(initials = profile.initials)
            Text(
                text = profile.displayName,
                style = KptTheme.typography.headlineMedium,
                color = KptTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(AccountHolderTestTags.DISPLAY_NAME),
            )
            Text(
                text = profile.roleLabel,
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.secondary,
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
            .size(DesignToken.sizes.avatarXLarge)
            .background(color = KptTheme.colorScheme.primaryContainer, shape = CircleShape)
            .testTag(AccountHolderTestTags.AVATAR)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = KptTheme.typography.headlineSmall,
            color = KptTheme.colorScheme.onPrimaryContainer,
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
            color = KptTheme.colorScheme.surfaceContainer,
            shape = KptTheme.shapes.medium,
            border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
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
        style = KptTheme.typography.titleSmall,
        color = KptTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(bottom = KptTheme.spacing.sm)
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
            .padding(KptTheme.spacing.md)
            .testTag(testTag)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignToken.sizes.iconMedium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
            Text(
                text = labelText,
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
