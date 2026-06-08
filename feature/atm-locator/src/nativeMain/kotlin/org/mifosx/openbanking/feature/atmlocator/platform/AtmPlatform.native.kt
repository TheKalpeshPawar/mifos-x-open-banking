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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.mifosx.openbanking.feature.atmlocator.ui.AtmRow
import org.mifosx.openbanking.feature.atmlocator.ui.MapCoordinate
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject

@Composable
actual fun AtmMapView(
    center: MapCoordinate?,
    atms: List<AtmRow>,
    selectedAtmId: String,
    onAtmSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(center?.longitude ?: 0.0, center?.latitude ?: 0.0),
            zoom = if (center != null) MAP_ZOOM else WORLD_ZOOM,
        ),
    )
    val selected = atms.firstOrNull { it.id == selectedAtmId }
    LaunchedEffect(selectedAtmId) {
        selected?.let {
            cameraState.animateTo(CameraPosition(target = Position(it.longitude, it.latitude), zoom = FOCUS_ZOOM))
        }
    }
    val markerColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    MaplibreMap(
        baseStyle = BaseStyle.Uri(OSM_STYLE_URL),
        cameraState = cameraState,
        modifier = modifier,
    ) {
        CircleLayer(
            id = "atm-markers",
            source = rememberGeoJsonSource(data = atms.toFeatures()),
            color = const(markerColor),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
        )
        CircleLayer(
            id = "atm-selected",
            source = rememberGeoJsonSource(data = listOfNotNull(selected).toFeatures()),
            color = const(selectedColor),
            radius = const(11.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(3.dp),
        )
    }
}

private fun List<AtmRow>.toFeatures(): GeoJsonData.Features = GeoJsonData.Features(
    FeatureCollection(
        map { Feature(Point(Position(it.longitude, it.latitude)), properties = buildJsonObject { }) },
    ),
)

actual fun isAtmLocatorSupported(): Boolean = true

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceLocationRequester(onResult: (MapCoordinate?) -> Unit): () -> Unit {
    val manager = remember { CLLocationManager() }
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                onResult(location?.coordinate?.useContents { MapCoordinate(latitude, longitude) })
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                onResult(null)
            }
        }
    }
    DisposableEffect(Unit) {
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }
    return {
        manager.requestWhenInUseAuthorization()
        manager.requestLocation()
    }
}

private const val OSM_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private const val MAP_ZOOM = 12.0

private const val FOCUS_ZOOM = 15.0

private const val WORLD_ZOOM = 1.0
