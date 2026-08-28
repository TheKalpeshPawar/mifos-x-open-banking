/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.isStale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Compact status banner shown above content. Status is encoded by background
 * color (M3 semantic containers — theme-respecting in light/dark/dynamic):
 *
 * - **FRESH** → hidden (no banner)
 * - **STALE_OFFLINE** (no connectivity, cached data) → `errorContainer` background,
 *   `CloudOff` icon, "Offline · Updated 5m ago"
 * - **STALE_FAILED** (online, last refresh failed) → `errorContainer` background,
 *   `SyncProblem` icon, "Couldn't refresh · Updated 5m ago"
 * - **UPDATING** (refresh in flight) → `primaryContainer` background,
 *   `Sync` icon, linear progress on top, "Refreshing · Last updated 5m ago"
 * - **JustReconnected** (transient, after a stale → non-stale transition) →
 *   `tertiaryContainer` background, `CloudDone` icon, "Connected · Refreshing
 *   data", auto-hides after [reconnectedVisibilityDuration]
 *
 * The "just reconnected" state is DERIVED inside this composable by observing
 * `freshness` transitions — no separate composable, no NetworkMonitor wiring.
 *
 * @param freshness Current data freshness state.
 * @param fetchedAt Wall-clock instant of last successful fetch for human-readable timestamp.
 * @param staleLabel Custom message for both stale states. Null uses the default with timestamp.
 * @param updatingLabel Custom updating message. Null uses "Refreshing...".
 * @param reconnectedLabel Custom message for the transient just-reconnected banner.
 * @param reconnectedVisibilityDuration How long the just-reconnected banner stays
 *   visible after the stale → non-stale transition. Default 3 seconds.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DataFreshnessIndicator(
    freshness: DataFreshness,
    modifier: Modifier = Modifier,
    fetchedAt: Instant? = null,
    staleLabel: String? = null,
    updatingLabel: String? = null,
    reconnectedLabel: String? = null,
    reconnectedVisibilityDuration: Duration = 3.seconds,
) {
    var showReconnected by remember { mutableStateOf(false) }
    var prevFreshness by remember { mutableStateOf(freshness) }

    LaunchedEffect(freshness) {
        val previous = prevFreshness
        prevFreshness = freshness
        if (previous.isStale && !freshness.isStale) {
            showReconnected = true
            try {
                delay(reconnectedVisibilityDuration)
            } finally {
                showReconnected = false
            }
        }
    }

    val display = deriveDisplayState(freshness, showReconnected)

    AnimatedVisibility(
        visible = display != DisplayState.Hidden,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier,
    ) {
        when (display) {
            DisplayState.Offline -> OfflineBanner(fetchedAt = fetchedAt, customLabel = staleLabel)
            DisplayState.RefreshFailed -> RefreshFailedBanner(fetchedAt = fetchedAt, customLabel = staleLabel)
            DisplayState.Updating -> UpdatingBanner(fetchedAt = fetchedAt, customLabel = updatingLabel)
            DisplayState.Reconnected -> ReconnectedBanner(customLabel = reconnectedLabel)
            DisplayState.Hidden -> Unit
        }
    }
}

/**
 * Visible-state representation. Derived from [DataFreshness] + the transient
 * "just reconnected" flag. Internal so [DataFreshnessIndicatorStateTest] can
 * exercise [deriveDisplayState] directly.
 */
internal enum class DisplayState {
    Hidden,
    Offline,
    RefreshFailed,
    Updating,
    Reconnected,
}

/** Pure helper — directly testable. */
internal fun deriveDisplayState(
    freshness: DataFreshness,
    showReconnected: Boolean,
): DisplayState = when {
    showReconnected -> DisplayState.Reconnected
    freshness == DataFreshness.STALE_OFFLINE -> DisplayState.Offline
    freshness == DataFreshness.STALE_FAILED -> DisplayState.RefreshFailed
    freshness == DataFreshness.UPDATING -> DisplayState.Updating
    else -> DisplayState.Hidden
}

@OptIn(ExperimentalTime::class)
@Composable
private fun OfflineBanner(
    fetchedAt: Instant?,
    customLabel: String?,
) {
    StaleBanner(
        icon = Icons.Default.CloudOff,
        text = customLabel ?: buildOfflineText(fetchedAt),
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun RefreshFailedBanner(
    fetchedAt: Instant?,
    customLabel: String?,
) {
    StaleBanner(
        icon = Icons.Default.SyncProblem,
        text = customLabel ?: buildRefreshFailedText(fetchedAt),
    )
}

@Composable
private fun StaleBanner(
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun UpdatingBanner(
    fetchedAt: Instant?,
    customLabel: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = customLabel ?: buildUpdatingText(fetchedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ReconnectedBanner(customLabel: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = customLabel ?: RECONNECTED_TEXT,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@OptIn(ExperimentalTime::class)
internal fun buildOfflineText(fetchedAt: Instant?): String {
    if (fetchedAt == null) return "Offline"
    val age = kotlin.time.Clock.System.now() - fetchedAt
    return "Offline · Updated ${formatDurationAgo(age)}"
}

@OptIn(ExperimentalTime::class)
internal fun buildRefreshFailedText(fetchedAt: Instant?): String {
    if (fetchedAt == null) return "Couldn't refresh"
    val age = kotlin.time.Clock.System.now() - fetchedAt
    return "Couldn't refresh · Updated ${formatDurationAgo(age)}"
}

@OptIn(ExperimentalTime::class)
internal fun buildUpdatingText(fetchedAt: Instant?): String {
    if (fetchedAt == null) return "Refreshing…"
    val age = kotlin.time.Clock.System.now() - fetchedAt
    return "Refreshing · Last updated ${formatDurationAgo(age)}"
}

internal const val RECONNECTED_TEXT: String = "Connected · Refreshing data"

internal fun formatDurationAgo(duration: Duration): String {
    val seconds = duration.inWholeSeconds
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}
