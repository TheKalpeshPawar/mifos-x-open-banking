/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.data.banking.StandingOrdersRepository
import org.mifosx.openbanking.core.data.util.isUnsupportedForProduct
import org.mifosx.openbanking.core.data.util.obieMessage
import org.mifosx.openbanking.core.model.banking.StandingOrderItem
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the standing-orders list: the order cards.
 *
 * Navigation is not modelled as an action — the screen owns back through its `onBack` lambda,
 * matching direct-debits — so this stays a pure state machine over the order stream.
 *
 * All display formatting happens here. The composables receive finished strings, which keeps
 * currency and date rendering testable on the JVM without a Compose runtime.
 */
class StandingOrdersViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: StandingOrdersRepository,
) : BaseViewModel<StandingOrdersState, Nothing, StandingOrdersAction>(
    initialState = StandingOrdersState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.standingOrdersStream(state.accountId, viewModelScope)

    init {
        stream.state
            .emptyIfContent { summary -> summary.isEmpty }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StandingOrdersAction) {
        when (action) {
            StandingOrdersAction.RetryLoad -> stream.refresh()
        }
    }

    private fun ScreenState<StandingOrdersSummary>.toUiState(): StandingOrdersUiState = when (this) {
        is ScreenState.Content -> data.toUiState()
        // Checked before classification: a U000 refusal is a statement about the product, not a
        // failure, so it must not be routed to a retryable error kind.
        is ScreenState.Error -> if (error.isUnsupportedForProduct()) {
            StandingOrdersUiState.Unsupported(error.obieMessage().orEmpty())
        } else {
            StandingOrdersUiState.Error(classifyStandingOrdersError(error))
        }
        is ScreenState.NoNetwork -> StandingOrdersUiState.Error(StandingOrdersErrorKind.NetworkError)
        ScreenState.Unauthenticated -> StandingOrdersUiState.Error(StandingOrdersErrorKind.TokenExpired)
        ScreenState.Empty -> StandingOrdersUiState.Empty
        ScreenState.Loading -> StandingOrdersUiState.Loading
    }

    /**
     * A payload whose order list came back empty renders the empty state even though the stream
     * reported Content — the bank answering "no standing orders" is a real answer, not missing data.
     */
    private fun StandingOrdersSummary.toUiState(): StandingOrdersUiState = if (isEmpty) {
        StandingOrdersUiState.Empty
    } else {
        StandingOrdersUiState.Content(
            orders = items.map { it.toRowUi() },
        )
    }

    private fun StandingOrderItem.toRowUi(): StandingOrderRowUi = StandingOrderRowUi(
        standingOrderId = standingOrderId,
        payeeName = payeeName,
        statusLabel = statusCode,
        isActive = isActive,
        amountLabel = nextPaymentAmount
            .takeIf { it.isNotBlank() }
            ?.let { formatMoney(it, currency) }
            .orEmpty(),
        currencyLabel = currency,
        frequencyLabel = frequencyLabel,
        nextDateLabel = formatStandingOrderDate(nextPaymentDateTime),
        finalDateLabel = formatStandingOrderDate(finalPaymentDateTime),
        hasFinalPayment = hasFinalPayment,
        sortCodeLabel = creditorIdentification,
        referenceLabel = reference,
    )

    companion object {
        /** Must match the [StandingOrdersRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

/**
 * Formats an ISO-8601 instant as `1 Jul 2026`, the form the order cards show.
 *
 * Anything unparseable is returned unchanged rather than blanked: a value the bank did send is
 * more useful on screen in an odd format than silently dropped. A genuinely absent date stays
 * blank, and the card omits that line entirely.
 */
internal fun formatStandingOrderDate(isoDateTime: String): String {
    val segments = isoDateTime.substringBefore('T').split('-')
    val year = segments.getOrNull(0)?.takeIf { segments.size == DATE_SEGMENTS }
    val month = MONTH_ABBREVIATIONS.getOrNull(segments.getOrNull(1)?.toIntOrNull()?.minus(1) ?: -1)
    val day = segments.getOrNull(2)?.trimStart('0')?.takeIf { it.isNotEmpty() }
    return if (year == null || month == null || day == null) {
        isoDateTime
    } else {
        "$day $month $year"
    }
}

private const val DATE_SEGMENTS = 3
private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
