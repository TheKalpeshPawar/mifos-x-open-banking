/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.obp.UserProfile
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Profile ViewModel. Loads the authenticated user's OBP profile ([ProfileRepository.current])
 * into a single [ScreenState] for read-only display (the form fields are non-editable). Log Out
 * flips [UserDataRepository.setIsAuthenticated] to false; RootNavViewModel observes `userData` and
 * routes back to the auth flow — no nav callback needed.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<ProfileContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<ProfileContent>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = ScreenState.Loading
        viewModelScope.launch {
            profileRepository.current()
                .onSuccess { profile ->
                    _uiState.value = ScreenState.Content(profile.toContent(), DataFreshness.FRESH)
                }
                .onFailure { error ->
                    _uiState.value = ScreenState.Error(error, isNetworkError = true)
                }
        }
    }

    fun onLogout() {
        viewModelScope.launch { userDataRepository.setIsAuthenticated(false) }
    }
}

/** Read-only content for the Profile screen. */
@Immutable
data class ProfileContent(
    val displayName: String,
    val initials: String,
    val fullName: String,
    val email: String,
    val phone: String,
)

internal fun UserProfile.toContent(): ProfileContent {
    val name = username.ifBlank { email.substringBefore('@') }
    return ProfileContent(
        displayName = name,
        initials = initialsOf(name),
        fullName = name,
        email = email,
        phone = "",
    )
}

/** Up to two uppercase initials from a display name. Blank when no name is available. */
internal fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
