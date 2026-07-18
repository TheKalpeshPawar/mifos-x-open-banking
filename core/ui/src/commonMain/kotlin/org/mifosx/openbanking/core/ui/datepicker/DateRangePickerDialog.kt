/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.ui.datepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Brand-colored custom date-range dialog: From/To fields (tap to choose which end the
 * calendar edits), month + year dropdown selectors, a day grid, and pill actions. The
 * whole surface uses the theme's primary/onPrimary pair so it follows light/dark themes.
 *
 * Reusable: any screen needing a date range can drop this in — [maxDate] disables future
 * days, [yearRange] bounds the year dropdown, and labels are customizable.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onApply: (from: LocalDate, to: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Custom Date Range",
    applyLabel: String = "Apply Date Range",
    dismissLabel: String = "Cancel",
    initialFrom: LocalDate? = null,
    initialTo: LocalDate? = null,
    maxDate: LocalDate? = null,
    yearRange: IntRange = 2000..2035,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var from by remember { mutableStateOf(initialFrom) }
    var to by remember { mutableStateOf(initialTo) }
    var activeEnd by remember { mutableStateOf(RangeEnd.FROM) }
    var visibleMonth by remember {
        val anchor = initialFrom ?: initialTo ?: maxDate ?: today
        mutableStateOf(LocalDate(anchor.year, anchor.month, 1))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column {
                RangeHeaderBar(title = title, onDismiss = onDismiss)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    RangeDateField(
                        label = "From",
                        date = from,
                        active = activeEnd == RangeEnd.FROM,
                        modifier = Modifier.weight(1f),
                        onClick = { activeEnd = RangeEnd.FROM },
                    )
                    Spacer(Modifier.width(16.dp))
                    RangeDateField(
                        label = "To",
                        date = to,
                        active = activeEnd == RangeEnd.TO,
                        modifier = Modifier.weight(1f),
                        onClick = { activeEnd = RangeEnd.TO },
                    )
                }
                Spacer(Modifier.height(16.dp))
                RangeCalendarCard(
                    visibleMonth = visibleMonth,
                    from = from,
                    to = to,
                    today = today,
                    maxDate = maxDate,
                    activeEnd = activeEnd,
                    yearRange = yearRange,
                    onMonthChanged = { visibleMonth = it },
                    onDayPicked = { day ->
                        if (activeEnd == RangeEnd.FROM) {
                            from = day
                            if (to != null && to!! < day) to = null
                            activeEnd = RangeEnd.TO
                        } else {
                            to = day
                        }
                    },
                )
                Spacer(Modifier.height(20.dp))
                RangeActionsRow(
                    dismissLabel = dismissLabel,
                    applyLabel = applyLabel,
                    applyEnabled = from != null && to != null && from!! <= to!!,
                    onDismiss = onDismiss,
                    onApply = { onApply(from!!, to!!) },
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private enum class RangeEnd { FROM, TO }

@Composable
private fun RangeHeaderBar(title: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f))
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun RangeDateField(
    label: String,
    date: LocalDate?,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary.copy(alpha = 0.9f),
                    )
                    Text(
                        " *",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.errorContainer,
                    )
                }
                Text(
                    date?.let(::formatSlashDate) ?: "DD/MM/YYYY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (date != null) onPrimary else onPrimary.copy(alpha = 0.45f),
                )
            }
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = onPrimary.copy(alpha = if (active) 1f else 0.7f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 2.dp else 1.dp)
                .background(onPrimary.copy(alpha = if (active) 1f else 0.4f)),
        )
    }
}

private enum class SelectorMode { DAYS, MONTHS, YEARS }

@Composable
private fun RangeCalendarCard(
    visibleMonth: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
    today: LocalDate,
    maxDate: LocalDate?,
    activeEnd: RangeEnd,
    yearRange: IntRange,
    onMonthChanged: (LocalDate) -> Unit,
    onDayPicked: (LocalDate) -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    var mode by remember { mutableStateOf(SelectorMode.DAYS) }
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(onPrimary.copy(alpha = 0.08f)),
    ) {
        SelectorTabs(
            visibleMonth = visibleMonth,
            mode = mode,
            onModeChanged = { mode = it },
        )
        when (mode) {
            SelectorMode.DAYS -> {
                WeekdayHeader()
                DayGrid(
                    visibleMonth = visibleMonth,
                    from = from,
                    to = to,
                    today = today,
                    maxDate = maxDate,
                    activeEnd = activeEnd,
                    onDayPicked = onDayPicked,
                )
            }
            SelectorMode.MONTHS -> MonthGrid(
                visibleMonth = visibleMonth,
                maxDate = maxDate,
                onMonthPicked = { month ->
                    onMonthChanged(LocalDate(visibleMonth.year, month, 1))
                    mode = SelectorMode.DAYS
                },
            )
            SelectorMode.YEARS -> YearGrid(
                visibleMonth = visibleMonth,
                yearRange = yearRange,
                maxDate = maxDate,
                onYearPicked = { year ->
                    onMonthChanged(LocalDate(year, visibleMonth.month, 1))
                    mode = SelectorMode.MONTHS
                },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * The month / year header tabs: the side whose grid is NOT open carries a lighter band;
 * tapping a side opens its grid (tapping again returns to the day view).
 */
@Composable
private fun SelectorTabs(
    visibleMonth: LocalDate,
    mode: SelectorMode,
    onModeChanged: (SelectorMode) -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        TabHalf(
            text = MONTH_NAMES[visibleMonth.month.number - 1],
            expanded = mode == SelectorMode.MONTHS,
            alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
            onClick = {
                onModeChanged(if (mode == SelectorMode.MONTHS) SelectorMode.DAYS else SelectorMode.MONTHS)
            },
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(onPrimary.copy(alpha = 0.25f)),
        )
        TabHalf(
            text = "${visibleMonth.year}",
            expanded = mode == SelectorMode.YEARS,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.weight(1f),
            onClick = {
                onModeChanged(if (mode == SelectorMode.YEARS) SelectorMode.DAYS else SelectorMode.YEARS)
            },
        )
    }
}

@Composable
private fun TabHalf(
    text: String,
    expanded: Boolean,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        contentAlignment = alignment,
        modifier = modifier
            .background(if (expanded) Color.Transparent else onPrimary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.SemiBold,
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Back to days" else "Change",
            )
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: LocalDate,
    maxDate: LocalDate?,
    onMonthPicked: (Int) -> Unit,
) {
    val monthRows = remember { (1..MONTHS_IN_YEAR).toList().chunked(GRID_COLUMNS) }
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        monthRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { month ->
                    val enabled = maxDate == null ||
                        visibleMonth.year < maxDate.year ||
                        (visibleMonth.year == maxDate.year && month <= maxDate.month.number)
                    GridPill(
                        text = MONTH_NAMES[month - 1].take(3),
                        selected = month == visibleMonth.month.number,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onMonthPicked(month) },
                    )
                }
            }
        }
    }
}

/**
 * Year selection grid for the calendar card.
 *
 * Shows one page of 12 years arranged column-major (each column runs top-to-bottom
 * before wrapping to the next), with paging arrows below to move to earlier or later
 * windows. Pages are clamped to [yearRange]; years past [maxDate] render disabled.
 * Picking a year calls [onYearPicked].
 */
@Composable
private fun YearGrid(
    visibleMonth: LocalDate,
    yearRange: IntRange,
    maxDate: LocalDate?,
    onYearPicked: (Int) -> Unit,
) {
    val pageSize = GRID_COLUMNS * GRID_ROWS
    var pageStart by remember {
        mutableStateOf(yearRange.first + ((visibleMonth.year - yearRange.first) / pageSize) * pageSize)
    }
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        repeat(GRID_ROWS) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(GRID_COLUMNS) { colIndex ->
                    val year = pageStart + colIndex * GRID_ROWS + rowIndex
                    val inRange = year in yearRange
                    val enabled = inRange && (maxDate == null || year <= maxDate.year)
                    GridPill(
                        text = if (inRange) "$year" else "",
                        selected = year == visibleMonth.year,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onYearPicked(year) },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = { pageStart -= pageSize },
                enabled = pageStart > yearRange.first,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Earlier years",
                    tint = onPrimary.copy(alpha = if (pageStart > yearRange.first) 1f else 0.35f),
                )
            }
            Spacer(Modifier.width(24.dp))
            IconButton(
                onClick = { pageStart += pageSize },
                enabled = pageStart + pageSize <= yearRange.last,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Later years",
                    tint = onPrimary.copy(alpha = if (pageStart + pageSize <= yearRange.last) 1f else 0.35f),
                )
            }
        }
    }
}

@Composable
private fun GridPill(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(contentAlignment = Alignment.Center, modifier = modifier.padding(vertical = 14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) onPrimary else Color.Transparent)
                .clickable(enabled = enabled && text.isNotEmpty(), onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    !enabled -> onPrimary.copy(alpha = 0.35f)
                    else -> onPrimary
                },
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Text(
                day,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayGrid(
    visibleMonth: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
    today: LocalDate,
    maxDate: LocalDate?,
    activeEnd: RangeEnd,
    onDayPicked: (LocalDate) -> Unit,
) {
    val daysInMonth = visibleMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
    val leadingBlanks = visibleMonth.dayOfWeek.isoDayNumber - 1
    val cells: List<LocalDate?> =
        List(leadingBlanks) { null } + (1..daysInMonth).map { LocalDate(visibleMonth.year, visibleMonth.month, it) }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                from = from,
                                to = to,
                                isToday = day == today,
                                enabled = dayEnabled(day, from, to, maxDate, activeEnd),
                                onClick = { onDayPicked(day) },
                            )
                        }
                    }
                }
                repeat(DAYS_PER_WEEK - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private fun dayEnabled(
    day: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
    maxDate: LocalDate?,
    activeEnd: RangeEnd,
): Boolean = when {
    maxDate != null && day > maxDate -> false
    activeEnd == RangeEnd.TO && from != null && day < from -> false
    activeEnd == RangeEnd.FROM && to != null && day > to -> false
    else -> true
}

@Composable
private fun DayCell(
    day: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val selected = day == from || day == to
    val inRange = from != null && to != null && day > from && day < to
    val background = when {
        selected -> onPrimary
        inRange -> onPrimary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = when {
        selected -> MaterialTheme.colorScheme.primary
        !enabled -> onPrimary.copy(alpha = 0.35f)
        else -> onPrimary
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.dp, onPrimary.copy(alpha = 0.6f), CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            "${day.day}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun RangeActionsRow(
    dismissLabel: String,
    applyLabel: String,
    applyEnabled: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onPrimary),
            border = BorderStroke(1.dp, colorScheme.onPrimary),
            modifier = Modifier.weight(1f),
        ) { Text(dismissLabel) }
        Spacer(Modifier.width(16.dp))
        Button(
            onClick = onApply,
            enabled = applyEnabled,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.onPrimary,
                contentColor = colorScheme.primary,
                disabledContainerColor = colorScheme.onPrimary.copy(alpha = 0.5f),
                disabledContentColor = colorScheme.primary.copy(alpha = 0.6f),
            ),
            modifier = Modifier.weight(1f),
        ) { Text(applyLabel) }
    }
}

/** "2026-06-07" → "07/06/2026". */
private fun formatSlashDate(date: LocalDate): String {
    val dd = date.day.toString().padStart(2, '0')
    val mm = date.month.number.toString().padStart(2, '0')
    return "$dd/$mm/${date.year}"
}

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private const val DAYS_PER_WEEK = 7
private const val MONTHS_IN_YEAR = 12
private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 4
