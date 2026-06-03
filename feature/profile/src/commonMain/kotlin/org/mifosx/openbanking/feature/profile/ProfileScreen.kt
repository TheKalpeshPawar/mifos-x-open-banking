/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.profile.ui.ProfileContent
import org.mifosx.openbanking.feature.profile.ui.ProfileViewModel
import template.core.base.store.screen.ScreenState

/**
 * Profile — stateful container binding [ProfileViewModel]'s [ScreenState] to the form body,
 * styled to the rendered design preview (`idea-layer/screens/profile/preview/viewing.html`).
 * All colors come from [MaterialTheme.colorScheme] roles so light and dark match by construction.
 */
@Composable
internal fun ProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    KptScaffold(
        title = "Profile",
        onNavigationIconClick = onBackClick,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val s = state) {
            is ScreenState.Loading -> ProfileLoading()

            is ScreenState.Content -> ProfileForm(
                content = s.data,
                onLogout = viewModel::onLogout,
            )

            else -> ProfileError(onRetry = viewModel::onRetry)
        }
    }
}

@Composable
private fun ProfileForm(
    content: ProfileContent,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        AvatarSection(initials = content.initials, displayName = content.displayName)

        FormCard(content = content)

        Spacer(Modifier.height(20.dp))

        // Disabled: OBP exposes no working change/reset-password endpoint. Same width as the
        // Personal Information card above (both inset 20.dp + fillMaxWidth).
        OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
        ) {
            Text("Change Password", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = onLogout,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Text("Log Out", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AvatarSection(initials: String, displayName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = 2.dp)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Change profile photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FormCard(content: ProfileContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        ProfileField(label = "Full Name", value = content.fullName)
        ProfileField(label = "Email Address", value = content.email)
        ProfileField(label = "Phone Number", value = content.phone)
    }
}

/** Read-only labelled value (disabled text field — display only). */
@Composable
private fun ProfileField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = {},
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileLoading() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Could not load your profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = MaterialTheme.colorScheme.primary)
        }
    }
}
