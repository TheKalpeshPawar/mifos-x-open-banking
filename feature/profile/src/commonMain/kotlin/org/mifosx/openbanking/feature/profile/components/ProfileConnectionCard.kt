/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.profile.ProfileSectionHeader
import org.mifosx.openbanking.feature.profile.ProfileTestTags
import org.mifosx.openbanking.feature.profile.generated.resources.Res
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_button_manage_consent
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_button_renew_consent
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_connected_to_bank
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_consent_expires_in_days
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_expiry_banner_accessibility
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_expiry_banner_body
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_label_consent_expires
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_label_status
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_permission_granted_accessibility
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_permissions_header
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_permissions_hide_accessibility
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_permissions_show_accessibility
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_section_connection
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_authorised
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_awaiting
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_consumed
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_expired
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_rejected
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_status_revoked
import org.mifosx.openbanking.feature.profile.ui.ProfileAction
import org.mifosx.openbanking.feature.profile.ui.ProfilePermissionUi
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState

private val CARD_RADIUS = 12.dp
private val ROW_PADDING = 16.dp
private val ROW_ICON_SIZE = 24.dp
private val PERMISSION_ICON_SIZE = 20.dp
private val ROW_ICON_GAP = 16.dp
private val ROW_LINE_GAP = 4.dp
private val BANNER_PADDING = 16.dp
private val BANNER_LINE_GAP = 12.dp
private const val BORDER_WIDTH = 1

/**
 * The Open Banking connection: which bank, the consent's status and expiry, the permissions it
 * carries, and the way to manage it.
 *
 * The expiry banner sits above the card rather than inside it because it is a call to act, not a
 * property of the connection — burying it among the metadata rows is how a consent lapses unnoticed.
 */
@Composable
internal fun ProfileConnectionCard(
    content: ProfileUiState.Content,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(
            text = stringResource(Res.string.feature_profile_section_connection),
            testTag = ProfileTestTags.CONNECTION_HEADER,
        )
        if (content.isExpiring) {
            ExpiryBanner(
                daysRemaining = content.daysRemaining,
                onRenew = { onAction(ProfileAction.RenewConsent) },
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(CARD_RADIUS),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProfileTestTags.CONNECTION_CARD),
        ) {
            Column {
                ConnectionBankRow()
                HorizontalDivider()
                ConnectionDetailRow(
                    icon = Icons.Filled.Verified,
                    label = Res.string.feature_profile_label_status,
                    value = stringResource(content.connection.status.labelResource()),
                    valueTestTag = ProfileTestTags.STATUS_VALUE,
                    rowTestTag = ProfileTestTags.STATUS_ROW,
                )
                ExpiryRow(content = content)
                PermissionsBlock(
                    permissions = content.permissions,
                    expanded = content.arePermissionsExpanded,
                    onToggle = { onAction(ProfileAction.TogglePermissions) },
                )
                ManageConsentRow(onManage = { onAction(ProfileAction.ManageConsent) })
            }
        }
    }
}

/**
 * The warning strip.
 *
 * Bound to the tertiary role rather than a literal amber: the palette owns what "caution, not
 * failure" looks like in both light and dark, and a hardcoded hex would be legible in exactly one
 * of them.
 */
@Composable
private fun ExpiryBanner(
    daysRemaining: Int,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_profile_expiry_banner_accessibility)
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(CARD_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = BANNER_LINE_GAP)
            .testTag(ProfileTestTags.EXPIRY_BANNER)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(BANNER_PADDING),
            verticalArrangement = Arrangement.spacedBy(BANNER_LINE_GAP),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(ROW_ICON_SIZE),
                )
                Text(
                    text = stringResource(
                        Res.string.feature_profile_expiry_banner_body,
                        daysRemaining,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.testTag(ProfileTestTags.EXPIRY_BANNER_BODY),
                )
            }
            OutlinedButton(
                onClick = onRenew,
                border = BorderStroke(BORDER_WIDTH.dp, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.testTag(ProfileTestTags.RENEW_CONSENT_BUTTON),
            ) {
                Text(
                    text = stringResource(Res.string.feature_profile_button_renew_consent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ConnectionBankRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ROW_PADDING)
            .testTag(ProfileTestTags.CONNECTION_BANK_ROW),
        horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
        Text(
            text = stringResource(Res.string.feature_profile_connected_to_bank),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The expiry row, tinted and annotated with the day count once the consent is close to lapsing.
 *
 * Hidden entirely when the consent carried no expiry — HSBC does not always send one, and an empty
 * "Consent Expires" row would read as a value the app failed to load.
 */
@Composable
private fun ExpiryRow(content: ProfileUiState.Content, modifier: Modifier = Modifier) {
    if (content.connection.expiryLabel.isBlank()) return
    val value = if (content.isExpiring) {
        stringResource(
            Res.string.feature_profile_consent_expires_in_days,
            content.connection.expiryLabel,
            content.daysRemaining,
        )
    } else {
        content.connection.expiryLabel
    }
    ConnectionDetailRow(
        icon = Icons.Outlined.Schedule,
        label = Res.string.feature_profile_label_consent_expires,
        value = value,
        valueTestTag = ProfileTestTags.EXPIRY_VALUE,
        rowTestTag = ProfileTestTags.EXPIRY_ROW,
        tint = if (content.isExpiring) MaterialTheme.colorScheme.tertiary else null,
        modifier = modifier,
    )
}

/**
 * One metadata row of the connection card.
 *
 * [tint] recolours both the icon and the value together, so an urgent row reads as one signal
 * rather than a coloured glyph next to neutral text.
 */
@Composable
private fun ConnectionDetailRow(
    icon: ImageVector,
    label: StringResource,
    value: String,
    valueTestTag: String,
    rowTestTag: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val labelText = stringResource(label)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ROW_PADDING)
            .testTag(rowTestTag),
        horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: MaterialTheme.colorScheme.primary,
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
                color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(valueTestTag),
            )
        }
    }
}

/**
 * The granted-permissions section: a header that doubles as the reveal control, and the list it
 * hides.
 *
 * Collapsed by default because the full OBIE scope is twenty-odd rows — enough to bury the Manage
 * Consent button below it. The list is a real disclosure, not a menu: it expands in place, pushing
 * the rows that follow rather than floating over them.
 */
@Composable
private fun PermissionsBlock(
    permissions: List<ProfilePermissionUi>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PermissionsToggleRow(expanded = expanded, onToggle = onToggle)
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.testTag(ProfileTestTags.PERMISSIONS_LIST)) {
                permissions.forEach { permission ->
                    PermissionRow(permission = permission)
                }
            }
        }
    }
}

/**
 * The "Permissions Granted" control. The chevron points down while the list is hidden and up while
 * it is shown, the standard reading of an inline disclosure.
 */
@Composable
private fun PermissionsToggleRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (expanded) {
            Res.string.feature_profile_permissions_hide_accessibility
        } else {
            Res.string.feature_profile_permissions_show_accessibility
        },
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(ROW_PADDING)
            .testTag(ProfileTestTags.PERMISSIONS_TOGGLE)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.feature_profile_permissions_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ProfileTestTags.PERMISSIONS_HEADER),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
    }
}

/**
 * One granted scope.
 *
 * The tick is tinted `primary`, not a success green: every row here is granted by construction, so
 * a success colour would be signalling an outcome where the design is only listing facts.
 */
@Composable
private fun PermissionRow(permission: ProfilePermissionUi, modifier: Modifier = Modifier) {
    val label = permission.label
    val description = stringResource(
        Res.string.feature_profile_permission_granted_accessibility,
        label,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ROW_PADDING)
            .testTag(ProfileTestTags.permissionRow(permission.id))
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PERMISSION_ICON_SIZE),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ManageConsentRow(onManage: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(ROW_PADDING)) {
        FilledTonalButton(
            onClick = onManage,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProfileTestTags.MANAGE_CONSENT_BUTTON),
        ) {
            Text(
                text = stringResource(Res.string.feature_profile_button_manage_consent),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** The display label for a consent status. */
private fun ConsentStatus.labelResource(): StringResource = when (this) {
    ConsentStatus.Authorised -> Res.string.feature_profile_status_authorised
    ConsentStatus.Revoked -> Res.string.feature_profile_status_revoked
    ConsentStatus.Expired -> Res.string.feature_profile_status_expired
    ConsentStatus.AwaitingAuthorisation -> Res.string.feature_profile_status_awaiting
    ConsentStatus.Rejected -> Res.string.feature_profile_status_rejected
    ConsentStatus.Consumed -> Res.string.feature_profile_status_consumed
}
