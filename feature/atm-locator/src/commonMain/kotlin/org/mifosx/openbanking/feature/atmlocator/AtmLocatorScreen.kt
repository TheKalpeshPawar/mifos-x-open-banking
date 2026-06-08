/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifosx.openbanking.feature.atmlocator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.feature.atmlocator.platform.AtmMapView
import org.mifosx.openbanking.feature.atmlocator.platform.rememberDeviceLocationRequester
import org.mifosx.openbanking.feature.atmlocator.ui.AtmLocatorContent
import org.mifosx.openbanking.feature.atmlocator.ui.AtmLocatorViewModel
import org.mifosx.openbanking.feature.atmlocator.ui.AtmRow
import template.core.base.store.screen.ScreenState

/**
 * ATM & Branch Locator — a MapLibre map of the ATMs across the banks the user holds an
 * account at, above a searchable, distance-aware list. Tapping a row selects that ATM.
 * Reachable on Android and iOS only (hidden on desktop/web, which have no map).
 */
@Composable
fun AtmLocatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AtmLocatorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ATM & Branches",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(AtmLocatorTestTags.TITLE),
                    )
                },
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
                is ScreenState.Loading -> AtmCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Empty -> AtmEmptyState()
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> AtmErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> AtmContent(content = s.data, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AtmContent(content: AtmLocatorContent, viewModel: AtmLocatorViewModel) {
    val requestLocation = rememberDeviceLocationRequester { coordinate ->
        coordinate?.let { viewModel.onUserLocation(it.latitude, it.longitude) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AtmMapView(
            center = content.center,
            atms = content.atms,
            selectedAtmId = content.selectedAtmId,
            onAtmSelected = viewModel::onAtmSelected,
            modifier = Modifier.fillMaxWidth().height(260.dp).testTag(AtmLocatorTestTags.MAP),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = content.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text("Search city, postcode or address") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag(AtmLocatorTestTags.SEARCH),
            )
            IconButton(
                onClick = requestLocation,
                modifier = Modifier.testTag(AtmLocatorTestTags.NEAR_ME),
            ) {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = "Use my location",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            content.resultSummary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag(AtmLocatorTestTags.RESULT_HEADER),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        ) {
            items(content.atms, key = { it.id }) { atm ->
                AtmCard(atm, selected = atm.id == content.selectedAtmId, onClick = { viewModel.onAtmSelected(atm.id) })
            }
        }
    }
}

@Composable
private fun AtmCard(atm: AtmRow, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(AtmLocatorTestTags.atmRow(atm.id)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                atm.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (atm.hoursLabel.isNotBlank()) {
                Text(
                    atm.hoursLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (atm.distanceLabel.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(
                        atm.distanceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            AnimatedVisibility(visible = selected) {
                AtmDetail(atm)
            }
        }
    }
}

@Composable
private fun AtmDetail(atm: AtmRow) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (atm.addressLine.isNotBlank()) {
            AtmDetailRow(Icons.Filled.Place, atm.addressLine)
        }
        if (atm.accessible || atm.acceptsDeposits) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                if (atm.accessible) AtmInfoChip(Icons.Filled.Accessible, "Accessible")
                if (atm.acceptsDeposits) AtmInfoChip(Icons.Filled.Savings, "Deposits")
            }
        }
        if (atm.moreInfo.isNotBlank()) {
            Text(
                atm.moreInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        OutlinedButton(
            onClick = { uriHandler.openUri(directionsUri(atm)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Directions")
        }
    }
}

@Composable
private fun AtmDetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AtmInfoChip(icon: ImageVector, label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Native maps `geo:` intent centred on the ATM with its name as the marker label. */
private fun directionsUri(atm: AtmRow): String =
    "geo:${atm.latitude},${atm.longitude}?q=${atm.latitude},${atm.longitude}(${atm.name})"

@Composable
private fun AtmEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "No ATMs nearby",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "None of your banks have published ATM locations yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AtmErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Couldn't load ATMs",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp).testTag(AtmLocatorTestTags.RETRY),
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun AtmCentered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
