/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.atmlocator.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.atmlocator.ui.AtmRow
import org.mifosx.openbanking.feature.atmlocator.ui.MapCoordinate

/**
 * Interactive ATM map. Renders [atms] as pins on a MapLibre map centred on [center];
 * tapping a pin reports its id via [onAtmSelected]. Real on Android and iOS (MapLibre +
 * OpenStreetMap tiles, no API key); a non-interactive placeholder on desktop/web, where
 * the locator is hidden from navigation anyway.
 */
@Composable
expect fun AtmMapView(
    center: MapCoordinate?,
    atms: List<AtmRow>,
    selectedAtmId: String,
    onAtmSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
)

/** True on the platforms where the ATM locator ships (Android, iOS); false on desktop/web. */
expect fun isAtmLocatorSupported(): Boolean

/**
 * Remembers a device-location requester. Invoking the returned lambda asks for location
 * permission (when needed) and resolves the current position via [onResult] — null when
 * permission is denied, no fix is available, or the platform has no location services.
 */
@Composable
expect fun rememberDeviceLocationRequester(onResult: (MapCoordinate?) -> Unit): () -> Unit

/** Placeholder used by the platforms that do not ship an interactive map (desktop, web). */
@Composable
internal fun AtmMapUnavailable(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Map view is not available on this platform",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}
