/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.settings.ui.SettingsUiState
import org.mifosx.openbanking.feature.settings.ui.SettingsViewModel
import template.core.base.store.screen.ScreenState

/** Display labels for the selectable languages, keyed by locale code. */
private val LANGUAGE_OPTIONS = listOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "hi" to "Hindi",
    "ar" to "Arabic",
)

/**
 * Settings — stateful container binding [SettingsViewModel]'s [ScreenState] to a profile header
 * plus the Appearance / Notifications / Security / About sections, styled to the rendered design
 * preview (`idea-layer/screens/settings/preview`). Colours come from [MaterialTheme.colorScheme]
 * roles so light and dark match by construction.
 *
 * Unbuilt features (Push, Transaction Alerts, Biometric, Language) render disabled. Navigation is
 * delegated to NavController callbacks; a null callback means that destination is not built yet, so
 * the row renders disabled.
 */
@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null,
    onNavigateToTerms: (() -> Unit)? = null,
    onNavigateToPrivacy: (() -> Unit)? = null,
    onNavigateToLicenses: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resetMessage by viewModel.resetMessage.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    val email = (state as? ScreenState.Content)?.data?.profileEmail.orEmpty()

    KptScaffold(
        title = "Settings",
        onNavigationIconClick = onBackClick,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val s = state) {
            is ScreenState.Content -> SettingsContent(
                state = s.data,
                onNavigateToProfile = onNavigateToProfile,
                onThemeSelected = viewModel::onThemeConfigSelected,
                onLanguageSelected = viewModel::onLanguageSelected,
                onPushToggled = viewModel::onPushNotificationsToggled,
                onTransactionAlertsToggled = viewModel::onTransactionAlertsToggled,
                onBiometricToggled = viewModel::onBiometricToggled,
                onResetPassword = { showResetDialog = true },
                onAbout = onNavigateToAbout,
                onTerms = onNavigateToTerms,
                onPrivacy = onNavigateToPrivacy,
                onLicenses = onNavigateToLicenses,
                onSignOut = viewModel::onSignOut,
            )

            else -> SettingsLoading()
        }
    }

    if (showResetDialog) {
        ResetPasswordDialog(
            email = email,
            onConfirm = {
                showResetDialog = false
                viewModel.onResetPassword()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    resetMessage?.let { message ->
        ResetMessageDialog(message = message, onDismiss = viewModel::onResetMessageConsumed)
    }
}

@Composable
private fun ResetPasswordDialog(email: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset password?") },
        text = {
            Text(
                if (email.isBlank()) {
                    "We'll email you a link to reset your password."
                } else {
                    "We'll email a password reset link to $email."
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Send link") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ResetMessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password reset") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onNavigateToProfile: (() -> Unit)?,
    onThemeSelected: (DarkThemeConfig) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onPushToggled: () -> Unit,
    onTransactionAlertsToggled: () -> Unit,
    onBiometricToggled: () -> Unit,
    onResetPassword: () -> Unit,
    onAbout: (() -> Unit)?,
    onTerms: (() -> Unit)?,
    onPrivacy: (() -> Unit)?,
    onLicenses: (() -> Unit)?,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SettingsTestTags.ROOT)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileHeaderCard(
            name = state.profileName,
            email = state.profileEmail,
            initials = state.profileInitials,
            onClick = onNavigateToProfile,
        )

        // ── Appearance ──────────────────────────────────────────────────────
        SectionHeader("Appearance")
        SettingsCard {
            ThemeModeRows(selected = state.themeConfig, onSelect = onThemeSelected)
            RowDivider()
            LanguageRow(
                selected = state.selectedLanguage,
                onSelect = onLanguageSelected,
                enabled = false,
            )
        }

        // ── Notifications (delivery not built → disabled) ────────────────────
        SectionHeader("Notifications")
        SettingsCard {
            ToggleRow(
                leadingIcon = Icons.Outlined.Notifications,
                label = "Push Notifications",
                description = "Receive alerts and updates from Mifos X",
                checked = state.isPushNotificationsEnabled,
                enabled = false,
                onToggle = onPushToggled,
                testTag = SettingsTestTags.PUSH_TOGGLE,
            )
            RowDivider()
            ToggleRow(
                leadingIcon = Icons.Outlined.ReceiptLong,
                label = "Transaction Alerts",
                description = "Notify me for every debit and credit activity",
                checked = state.isTransactionAlertsEnabled,
                enabled = false,
                onToggle = onTransactionAlertsToggled,
                activeColor = MaterialTheme.colorScheme.secondary,
                testTag = SettingsTestTags.TRANSACTION_ALERTS_TOGGLE,
            )
        }

        // ── Security ────────────────────────────────────────────────────────
        SectionHeader("Security")
        SettingsCard {
            ToggleRow(
                leadingIcon = Icons.Outlined.Fingerprint,
                label = "Biometric Login",
                description = "Use fingerprint or face ID to sign in faster",
                checked = state.isBiometricLoginEnabled,
                enabled = false,
                onToggle = onBiometricToggled,
                testTag = SettingsTestTags.BIOMETRIC_TOGGLE,
            )
            RowDivider()
            // OBP has no in-app change-password; this requests an email-based reset instead.
            NavRow(
                leadingIcon = Icons.Outlined.Lock,
                label = "Reset Password",
                description = "Email yourself a password reset link",
                onClick = onResetPassword,
                testTag = SettingsTestTags.CHANGE_PASSWORD_ROW,
            )
        }

        // ── About (destinations not built → disabled) ────────────────────────
        SectionHeader("About")
        AboutCard(
            leadingIcon = Icons.Outlined.Info,
            label = "About Mifos X Open Banking",
            onClick = onAbout,
            testTag = SettingsTestTags.ABOUT_LINK,
        )
        AboutCard(
            leadingIcon = Icons.AutoMirrored.Outlined.Article,
            label = "Terms of Service",
            onClick = onTerms,
            testTag = SettingsTestTags.TERMS_LINK,
        )
        AboutCard(
            leadingIcon = Icons.Outlined.PrivacyTip,
            label = "Privacy Policy",
            onClick = onPrivacy,
            testTag = SettingsTestTags.PRIVACY_LINK,
        )
        AboutCard(
            leadingIcon = Icons.Outlined.Gavel,
            label = "Open-source Licences",
            onClick = onLicenses,
            testTag = SettingsTestTags.LICENSES_LINK,
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(SettingsTestTags.SIGN_OUT),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Sign out",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "Mifos X Open Banking  ·  ${state.appVersion}",
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    email: String,
    initials: String,
    onClick: (() -> Unit)?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsTestTags.PROFILE_HEADER)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        // M3 tonal container pair — matches the preview: light-green card + dark text (light theme),
        // dark-green card + light text (dark theme). NOT the full-strength `primary` role.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials.ifBlank { "M" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Mifos User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            content()
        }
    }
}

/** A single tappable About entry rendered as its own card (matches the preview). */
@Composable
private fun AboutCard(
    leadingIcon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    testTag: String,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        NavRow(
            leadingIcon = leadingIcon,
            label = label,
            onClick = onClick,
            testTag = testTag,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun LeadingIconSlot(icon: ImageVector, enabled: Boolean) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * The three mutually-exclusive theme-mode rows. Each is a switch bound to whether [selected] equals
 * that [DarkThemeConfig]; turning one on persists that mode via [onSelect], so the other two reflect
 * off — exactly one mode is always active. Tapping the already-active row re-selects it (a no-op).
 */
@Composable
private fun ThemeModeRows(selected: DarkThemeConfig, onSelect: (DarkThemeConfig) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            leadingIcon = Icons.Outlined.BrightnessAuto,
            label = "Follow System",
            description = "Match your device's light or dark setting",
            checked = selected == DarkThemeConfig.FOLLOW_SYSTEM,
            onToggle = { onSelect(DarkThemeConfig.FOLLOW_SYSTEM) },
            testTag = SettingsTestTags.THEME_MODE_SYSTEM,
        )
        RowDivider()
        ToggleRow(
            leadingIcon = Icons.Outlined.LightMode,
            label = "Light Mode",
            description = "Always use the light theme",
            checked = selected == DarkThemeConfig.LIGHT,
            onToggle = { onSelect(DarkThemeConfig.LIGHT) },
            testTag = SettingsTestTags.THEME_MODE_LIGHT,
        )
        RowDivider()
        ToggleRow(
            leadingIcon = Icons.Outlined.DarkMode,
            label = "Dark Mode",
            description = "Always use the dark theme",
            checked = selected == DarkThemeConfig.DARK,
            onToggle = { onSelect(DarkThemeConfig.DARK) },
            testTag = SettingsTestTags.THEME_MODE_DARK,
        )
    }
}

@Composable
private fun ToggleRow(
    leadingIcon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    description: String? = null,
    enabled: Boolean = true,
    activeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            LeadingIconSlot(leadingIcon, enabled)
            Spacer(Modifier.width(12.dp))
            LabelGroup(label = label, description = description, enabled = enabled)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(checkedTrackColor = activeColor),
        )
    }
}

/**
 * A navigable preference row. When [onClick] is null the target screen is not yet built, so the
 * row renders disabled (dimmed, no chevron, not clickable).
 */
@Composable
private fun NavRow(
    leadingIcon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    testTag: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val enabled = onClick != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            LeadingIconSlot(leadingIcon, enabled)
            Spacer(Modifier.width(12.dp))
            LabelGroup(label = label, description = description, enabled = enabled)
        }
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/**
 * Language row. The dropdown + persistence logic is fully wired, but [enabled] gates interaction:
 * when false (locale switching not yet applied app-wide) the row is dimmed and tapping does
 * nothing. Flip [enabled] to true to re-activate the picker without touching its logic.
 */
@Composable
private fun LanguageRow(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = LANGUAGE_OPTIONS.firstOrNull { it.first == selected }?.second ?: "English"
    val valueColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            LeadingIconSlot(Icons.Outlined.Language, enabled)
            Spacer(Modifier.width(12.dp))
            LabelGroup(
                label = "Language",
                description = "Choose your preferred display language",
                enabled = enabled,
            )
        }
        Box {
            Row(
                modifier = Modifier
                    .testTag(SettingsTestTags.LANGUAGE_SELECT)
                    .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                )
                if (enabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LANGUAGE_OPTIONS.forEach { (code, labelText) ->
                    DropdownMenuItem(
                        text = { Text(labelText) },
                        onClick = {
                            expanded = false
                            onSelect(code)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelGroup(label: String, description: String?, enabled: Boolean) {
    val labelColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowDivider() {
    // Inset to start under the label (past the 36dp icon slot + 12dp gap) and use a fainter tone
    // than the card's outline border so the two don't read as the same line.
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp, top = 2.dp, bottom = 2.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
private fun SettingsLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(40.dp))
    }
}
