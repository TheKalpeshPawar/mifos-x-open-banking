/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.data.banking.AccountCapabilityRegistry
import org.mifosx.openbanking.core.data.banking.AccountDetailRepository
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.AccountDetailWithBalances
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineScreenStates
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the account-detail hub: account header, typed balance rows and the Explore chip row.
 *
 * Navigation is not modelled as an action here. The screen owns it through `onNavigate*` lambdas,
 * matching the accounts and transactions features, so the view model stays a pure state machine
 * over the merged detail-and-balances stream.
 *
 * All display formatting happens here — the composables receive finished strings.
 */
class AccountDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountDetailRepository,
    private val capabilityRegistry: AccountCapabilityRegistry,
) : BaseViewModel<AccountDetailState, Nothing, AccountDetailAction>(
    initialState = AccountDetailState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The two streams this screen renders, scoped to this view model. */
    private val detail = repository.detailStream(state.accountId, viewModelScope)
    private val balanceLines = repository.balanceLinesStream(state.accountId, viewModelScope)

    init {
        combineScreenStates(detail.state, balanceLines.state) { account, balances ->
            AccountDetailWithBalances(detail = account, balances = balances)
        }
            .combine(capabilityRegistry.unsupportedStream(state.accountId)) { screenState, unsupported ->
                screenState to unsupported
            }
            .onEach { (screenState, unsupported) ->
                updateState {
                    copy(
                        uiState = screenState.toUiState(),
                        availableChips = availableChipsFor(
                            productType = screenState.productTypeOrUnknown(),
                            unsupported = unsupported,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AccountDetailAction) {
        when (action) {
            AccountDetailAction.RetryLoad -> {
                detail.refresh()
                balanceLines.refresh()
            }
        }
    }

    /**
     * The product, once the account has actually loaded.
     *
     * Every other state reports [HsbcProductType.Unknown], which keeps the full chip set — those
     * states render no chips anyway, and treating "not loaded yet" as "unsupported" would make the
     * row flicker as the stream settles.
     */
    private fun ScreenState<AccountDetailWithBalances>.productTypeOrUnknown(): HsbcProductType =
        (this as? ScreenState.Content)?.data?.detail?.productType ?: HsbcProductType.Unknown

    private fun ScreenState<AccountDetailWithBalances>.toUiState(): AccountDetailUiState = when (this) {
        is ScreenState.Content -> data.toUiState()
        is ScreenState.Error -> error.toErrorState()
        is ScreenState.NoNetwork -> AccountDetailErrorKind.Network.toErrorState()
        ScreenState.Unauthenticated -> AccountDetailErrorKind.TokenExpired.toErrorState()
        ScreenState.Empty, ScreenState.Loading -> AccountDetailUiState.Loading
    }

    private fun AccountDetailWithBalances.toUiState(): AccountDetailUiState {
        val header = detail.toHeaderUi()
        return if (hasNoBalances) {
            AccountDetailUiState.Empty(header = header)
        } else {
            AccountDetailUiState.Content(
                header = header,
                balances = balances.map { it.toRowUi() },
            )
        }
    }

    private fun AccountDetail.toHeaderUi(): AccountHeaderUi = AccountHeaderUi(
        nickname = nickname,
        accountSubType = accountSubType.uppercase(),
        identificationLabel = buildIdentificationLabel(sortCode, accountNumber),
        currency = currency,
        servicerIdentification = servicerIdentification,
        lastUpdatedLabel = formatStatusTimestamp(statusUpdateDateTime),
    )

    private fun AccountBalanceLine.toRowUi(): BalanceRowUi = BalanceRowUi(
        type = type,
        amountLabel = "${groupThousands(amount)} $currency",
    )

    private fun Throwable.toErrorState(): AccountDetailUiState.Error =
        classifyAccountDetailError(this).toErrorState()

    private fun AccountDetailErrorKind.toErrorState(): AccountDetailUiState.Error =
        AccountDetailUiState.Error(kind = this, recoverable = recoverable)

    companion object {
        /** Must match the [AccountDetailRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

/**
 * Renders `400515` + `12345678` as `40-05-15 12345678`, the form the header card shows. Falls back
 * to whichever half is present so a partial identification still reads sensibly.
 */
internal fun buildIdentificationLabel(sortCode: String, accountNumber: String): String = listOf(
    sortCode.takeIf { it.isNotBlank() }?.let(::formatSortCode).orEmpty(),
    accountNumber,
).filter { it.isNotBlank() }.joinToString(" ")

/**
 * Inserts thousands separators into a raw OBIE decimal string, preserving the fractional part
 * exactly as the bank sent it. Non-numeric input is returned untouched rather than dropped, so an
 * unexpected payload still renders something truthful.
 */
internal fun groupThousands(amount: String): String {
    val negative = amount.startsWith("-")
    val unsigned = amount.removePrefix("-")
    val whole = unsigned.substringBefore('.')
    val fraction = unsigned.substringAfter('.', missingDelimiterValue = "")
    return if (whole.isEmpty() || !whole.all { it.isDigit() }) {
        amount
    } else {
        val grouped = whole.reversed().chunked(THOUSANDS_GROUP).joinToString(",").reversed()
        buildString {
            if (negative) append('-')
            append(grouped)
            if (fraction.isNotEmpty()) append('.').append(fraction)
        }
    }
}

/**
 * Formats an ISO-8601 instant as `28 Jun 2026, 18:30 UTC`. Anything unparseable is returned
 * unchanged so the header never renders an empty "Last updated" row for a value the bank did send.
 */
internal fun formatStatusTimestamp(isoDateTime: String): String {
    val datePart = isoDateTime.substringBefore('T')
    val timePart = isoDateTime.substringAfter('T', missingDelimiterValue = "")
    val segments = datePart.split('-')
    return if (segments.size != DATE_SEGMENTS || timePart.length < TIME_PREFIX) {
        isoDateTime
    } else {
        val month = MONTH_ABBREVIATIONS.getOrNull(segments[1].toIntOrNull()?.minus(1) ?: -1)
        if (month == null) {
            isoDateTime
        } else {
            "${segments[2].trimStart('0')} $month ${segments[0]}, ${timePart.take(TIME_PREFIX)} UTC"
        }
    }
}

private const val THOUSANDS_GROUP = 3
private const val DATE_SEGMENTS = 3
private const val TIME_PREFIX = 5
private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
