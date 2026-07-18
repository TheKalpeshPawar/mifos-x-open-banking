/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.atmlocator.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.atm.AtmRepository
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.model.obp.OpeningHours
import org.mifosx.openbanking.core.model.obp.PostalAddress
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ATM & Branch Locator ViewModel. Aggregates the ATMs of every bank the user holds an
 * account at (OBP exposes ATMs per bank), filters them by a free-text location query, and
 * — once the screen reports the device location via [onUserLocation] — sorts them nearest
 * first and labels each with its distance. The map centre follows the device location when
 * known, otherwise the centroid of the matched ATMs. Geolocation itself is the screen's
 * concern (platform location services); this ViewModel stays platform-free and is driven
 * entirely by [onUserLocation].
 */
class AtmLocatorViewModel(
    private val atmRepository: AtmRepository,
    private val accountsRepository: AccountsRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<ScreenState<AtmLocatorContent>> =
        combine(rawState, filters) { raw, f -> project(raw, f) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    fun onQueryChanged(query: String) = filters.update { it.copy(query = query) }

    /** Report the device location once permission is granted; re-sorts the list nearest first. */
    fun onUserLocation(latitude: Double, longitude: Double) =
        filters.update { it.copy(location = MapCoordinate(latitude, longitude)) }

    /** Toggles the expanded/selected ATM — tapping the open one closes it; only one is open. */
    fun onAtmSelected(atmId: String) =
        filters.update { it.copy(selectedAtmId = if (it.selectedAtmId == atmId) "" else atmId) }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val bankIds = accountsRepository.myAccounts()
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
                .map { it.bankId }
                .filter { it.isNotBlank() }
                .distinct()
            if (bankIds.isEmpty()) {
                rawState.value = RawState.Loaded(emptyList())
                return@launch
            }
            atmRepository.atmsForBanks(bankIds)
                .onSuccess { rawState.value = RawState.Loaded(it) }
                .onFailure { rawState.value = RawState.Failed(it) }
        }
    }

    private fun project(raw: RawState, f: Filters): ScreenState<AtmLocatorContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> if (raw.atms.isEmpty()) {
            ScreenState.Empty
        } else {
            ScreenState.Content(data = content(raw.atms, f), freshness = DataFreshness.FRESH)
        }
    }

    private fun content(atms: List<Atm>, f: Filters): AtmLocatorContent {
        val rows = atms.filter { matchesQuery(it, f.query) }.map { row(it, f.location) }
        val ordered = if (f.location != null) rows.sortedBy { it.distanceMeters ?: Double.MAX_VALUE } else rows
        return AtmLocatorContent(
            atms = ordered,
            query = f.query,
            hasLocation = f.location != null,
            resultSummary = summaryFor(ordered.size, f.location != null),
            selectedAtmId = f.selectedAtmId,
            center = centerFor(ordered, f.location),
        )
    }

    private fun row(atm: Atm, location: MapCoordinate?): AtmRow {
        val meters = location?.let {
            distanceMeters(it.latitude, it.longitude, atm.location.latitude, atm.location.longitude)
        }
        return AtmRow(
            id = atm.id,
            name = atm.name.ifBlank { "ATM" },
            addressLine = addressLine(atm.address),
            distanceLabel = meters?.let { distanceLabel(it) }.orEmpty(),
            distanceMeters = meters,
            latitude = atm.location.latitude,
            longitude = atm.location.longitude,
            hoursLabel = hoursLabel(atm.week),
            accessible = atm.accessible,
            acceptsDeposits = atm.acceptsDeposits,
            moreInfo = atm.moreInfo,
        )
    }

    private fun centerFor(rows: List<AtmRow>, location: MapCoordinate?): MapCoordinate? = when {
        location != null -> location
        rows.isEmpty() -> null
        else -> MapCoordinate(rows.map { it.latitude }.average(), rows.map { it.longitude }.average())
    }

    private data class Filters(
        val query: String = "",
        val location: MapCoordinate? = null,
        val selectedAtmId: String = "",
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val atms: List<Atm>) : RawState
    }
}

/** Display-ready content for the ATM Locator screen. */
@Immutable
data class AtmLocatorContent(
    val atms: List<AtmRow>,
    val query: String,
    val hasLocation: Boolean,
    val resultSummary: String,
    val selectedAtmId: String,
    val center: MapCoordinate?,
)

/** One display-ready ATM row. [distanceMeters] is null until the device location is known. */
@Immutable
data class AtmRow(
    val id: String,
    val name: String,
    val addressLine: String,
    val distanceLabel: String,
    val distanceMeters: Double?,
    val latitude: Double,
    val longitude: Double,
    val hoursLabel: String,
    val accessible: Boolean,
    val acceptsDeposits: Boolean,
    val moreInfo: String,
)

/** A geographic point the map can centre on. */
@Immutable
data class MapCoordinate(val latitude: Double, val longitude: Double)

private fun summaryFor(count: Int, hasLocation: Boolean): String = when {
    count == 0 -> "No matching ATMs"
    hasLocation -> "$count ATM${if (count == 1) "" else "s"} near you"
    else -> "$count ATM${if (count == 1) "" else "s"}"
}

private fun addressLine(address: PostalAddress): String =
    listOf(address.line1, address.line2, address.city, address.county, address.postCode)
        .filter { it.isNotBlank() }
        .joinToString(", ")

/** "Open 24 hours" when every day runs midnight-to-midnight, else a daily or "varies" summary. */
private fun hoursLabel(week: List<OpeningHours>): String = when {
    week.isEmpty() -> ""
    week.all { it.openingTime == "00:00" && (it.closingTime == "23:59" || it.closingTime == "24:00") } ->
        "Open 24 hours"
    week.distinctBy { it.openingTime to it.closingTime }.size == 1 ->
        "Daily ${week.first().openingTime}–${week.first().closingTime}"
    else -> "Hours vary by day"
}

private fun matchesQuery(atm: Atm, query: String): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true
    val haystack = with(atm) {
        "$name ${address.line1} ${address.line2} ${address.city} ${address.postCode}".lowercase()
    }
    return q.lowercase().split(' ').filter { it.isNotBlank() }.all { it in haystack }
}

private fun distanceLabel(meters: Double): String =
    if (meters < 1_000) "${meters.roundToInt()} m" else "${(meters / 100).roundToInt() / 10.0} km"

private const val EARTH_RADIUS_M = 6_371_000.0

private const val DEGREES_TO_RADIANS = 0.017453292519943295

/** Great-circle distance in metres between two lat/lng points. */
internal fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = (lat2 - lat1) * DEGREES_TO_RADIANS
    val dLng = (lng2 - lng1) * DEGREES_TO_RADIANS
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * DEGREES_TO_RADIANS) * cos(lat2 * DEGREES_TO_RADIANS) * sin(dLng / 2) * sin(dLng / 2)
    return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
}
