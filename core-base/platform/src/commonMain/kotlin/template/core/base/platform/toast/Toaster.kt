/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.platform.toast

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that shows a short transient platform message (a long-duration
 * Toast on Android; a no-op on platforms without a native toast). Toasts outlive the
 * current screen, so they suit confirmations fired right before navigating away.
 */
@Composable
expect fun rememberToastLauncher(): (String) -> Unit
