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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mifosx.openbanking.feature.atmlocator.ui.AtmRow
import org.mifosx.openbanking.feature.atmlocator.ui.MapCoordinate

@Composable
actual fun AtmMapView(
    center: MapCoordinate?,
    atms: List<AtmRow>,
    selectedAtmId: String,
    onAtmSelected: (String) -> Unit,
    modifier: Modifier,
) = AtmMapUnavailable(modifier)

actual fun isAtmLocatorSupported(): Boolean = false

@Composable
actual fun rememberDeviceLocationRequester(onResult: (MapCoordinate?) -> Unit): () -> Unit = { onResult(null) }
