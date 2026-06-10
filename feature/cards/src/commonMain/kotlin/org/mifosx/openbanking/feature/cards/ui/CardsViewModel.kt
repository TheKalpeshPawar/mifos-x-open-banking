/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.cards.CardsRepository
import org.mifosx.openbanking.core.model.obp.Card
import template.core.base.store.screen.ScreenState
import template.core.base.store.screen.combineContent
import template.core.base.store.screen.emptyIfContent

/**
 * My Cards ViewModel. The carousel is the offline-first [CardsRepository.userCardsStream]
 * (GET /obp/v7.0.0/cards — every card the signed-in user holds). As the user swipes the carousel
 * the screen calls [selectCard], and [CardsContent.selectedCardId] tracks the active card so the
 * details section reflects whichever card is shown.
 *
 * The card controls (freeze, set limit, view PIN, report lost, order new card) are deferred:
 * OBP exposes them only through management endpoints requiring the CanUpdateCardsForBank role.
 * Tapping one emits a "coming soon" message rather than calling a non-functional endpoint.
 */
class CardsViewModel(
    cardsRepository: CardsRepository,
) : ViewModel() {

    private val stream = cardsRepository.userCardsStream(viewModelScope)

    private val selectedCardNumber = MutableStateFlow("")

    private val deferredMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deferredActionMessages: SharedFlow<String> = deferredMessages.asSharedFlow()

    val uiState: StateFlow<ScreenState<CardsContent>> = stream.state
        .combineContent(selectedCardNumber) { cards, selected, _ ->
            CardsContent(
                cards = cards,
                selectedCardId = selected.ifBlank { cards.firstOrNull()?.bankCardNumber.orEmpty() },
            )
        }
        .emptyIfContent { it.cards.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    init {
        // Auto-select the first card once the carousel resolves.
        viewModelScope.launch {
            stream.state.collect { state ->
                if (state is ScreenState.Content && selectedCardNumber.value.isBlank()) {
                    state.data.firstOrNull()?.let(::selectCard)
                }
            }
        }
    }

    /** Swiped/tapped to a card — drives which card the details section reflects. */
    fun selectCard(card: Card) {
        selectedCardNumber.value = card.bankCardNumber
    }

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()

    /** Tapping any deferred card control surfaces a "coming soon" message. */
    fun onDeferredAction(control: String) {
        deferredMessages.tryEmit("$control is coming soon")
    }
}

/** Loaded content for the My Cards screen — the card carousel + the currently-selected card. */
@Immutable
data class CardsContent(
    val cards: List<Card>,
    val selectedCardId: String,
)
