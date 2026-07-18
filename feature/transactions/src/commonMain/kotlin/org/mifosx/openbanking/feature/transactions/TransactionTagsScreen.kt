/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.feature.transactions.ui.TransactionTagsContent
import org.mifosx.openbanking.feature.transactions.ui.TransactionTagsViewModel
import template.core.base.store.screen.ScreenState

/**
 * Tags & Notes for one transaction — real OBP v1.2.1 metadata. Tag chips add/remove
 * immediately; the note saves on Save and pops back. Receipt attachment is deferred:
 * OBP's image endpoint stores a URL, the device has nowhere to host an upload.
 */
@Composable
fun TransactionTagsScreen(
    bankId: String,
    accountId: String,
    transactionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionTagsViewModel = koinViewModel {
        parametersOf(bankId, accountId, transactionId)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(notice) {
        notice?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.onNoticeConsumed()
        }
    }
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tags & Notes", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> TagsCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> TagsErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Empty -> TagsErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> TagsContent(
                    content = s.data,
                    viewModel = viewModel,
                    onNotify = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TagsContent(
    content: TransactionTagsContent,
    viewModel: TransactionTagsViewModel,
    onNotify: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TagsHeaderCard(content)
        TagsSection(content, viewModel)
        TagsSectionDivider()
        TagsNotesSection(content, viewModel)
        TagsSectionDivider()
        TagsReceiptSection(onNotify)
        Button(
            onClick = viewModel::onSave,
            enabled = !content.isSaving,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag(TransactionTagsTestTags.SAVE_BUTTON),
        ) {
            if (content.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Save")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TagsHeaderCard(content: TransactionTagsContent) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(20.dp)
            .testTag(TransactionTagsTestTags.HEADER_CARD),
    ) {
        Text(
            content.merchant,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            content.amount,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            content.dateTime,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun TagsSection(content: TransactionTagsContent, viewModel: TransactionTagsViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            "Tags",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (content.tags.isNotEmpty()) {
            Text(
                "Tap a tag to remove it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                content.tags.forEach { chip ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.onRemoveTag(chip.id) },
                        label = { Text(chip.value) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove ${chip.value}",
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag(TransactionTagsTestTags.chip(chip.id)),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                "No tags added yet. Type a tag below and tap Add to categorise this transaction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = content.tagInput,
                onValueChange = viewModel::onTagInputChanged,
                placeholder = { Text("#groceries, #holiday…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag(TransactionTagsTestTags.TAG_INPUT),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = viewModel::onAddTag,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag(TransactionTagsTestTags.ADD_BUTTON),
            ) { Text("Add") }
        }
        if (content.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "SUGGESTED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                content.suggestions.forEach { value ->
                    SuggestionChip(
                        onClick = { viewModel.onSuggestionClick(value) },
                        label = { Text(value) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag(TransactionTagsTestTags.suggestion(value)),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TagsNotesSection(content: TransactionTagsContent, viewModel: TransactionTagsViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            "Notes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content.note,
            onValueChange = viewModel::onNoteChanged,
            placeholder = { Text("e.g. Weekly shop — bought extra for bank holiday") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 100.dp)
                .testTag(TransactionTagsTestTags.NOTES_FIELD),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TagsReceiptSection(onNotify: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Text(
            "Receipt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
                .clickable { onNotify("Receipt attachments are coming soon") }
                .testTag(TransactionTagsTestTags.RECEIPT_AREA),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                Icon(
                    Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap to attach a receipt image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onNotify("Receipt attachments are coming soon") },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
            modifier = Modifier.testTag(TransactionTagsTestTags.CAMERA_BUTTON),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Take Photo")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TagsSectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp),
    )
}

@Composable
private fun TagsErrorState(onRetry: () -> Unit) {
    TagsCentered {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Unable to load metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Could not load tags and notes. Check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun TagsCentered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
}
