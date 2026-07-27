/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.store.screen

import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.dataOrNull
import template.core.base.common.screen.hasContent
import template.core.base.store.submit.SubmitHandler
import template.core.base.store.submit.SubmitState

/**
 * Execute [block] only when [screenState] has loaded content, passing its data.
 * No-op when state is Loading / Error / NoNetwork / Empty — safe to call at any time.
 * Guards against premature submits before data arrives on edit screens.
 */
fun <T, R> SubmitHandler<R>.submitWhenContent(
    screenState: ScreenState<T>,
    block: suspend (data: T) -> R,
) {
    val data = screenState.dataOrNull ?: return
    submit { block(data) }
}

/**
 * True when the screen has content AND no submission is in-flight.
 *
 * Use to enable a submit button without collecting two flows separately:
 * ```kotlin
 * Button(enabled = screenState.canInteract(submitState)) { … }
 * ```
 */
fun <T, R> ScreenState<T>.canInteract(submitState: SubmitState<R>): Boolean =
    hasContent && submitState !is SubmitState.Submitting
