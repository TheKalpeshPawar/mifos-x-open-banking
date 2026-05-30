/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Branded launch visual (`RootNavState.Splash`).
 *
 * This is a STATELESS visual only. Session check + post-splash routing
 * (Auth / Authenticated / Onboarding) is owned by `RootNavViewModel`
 * (`cmp.navigation.rootnav`) via `RootNavState` — this composable never reads
 * auth state or decides a destination. See `screens/splash/ui.yaml` (reconciled
 * 2026-05-30) and `exports/splash/SPEC.md`.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // TODO(asset): replace the monogram placeholder with the real `mifos_logo`
            //  drawable once it lands in core/designsystem composeResources, then render
            //  it 120dp tinted MaterialTheme.colorScheme.primary per ui.yaml#splash_logo.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Mifos X application logo" },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(120.dp),
                ) {}
                Text(
                    text = "MX",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            Text(
                text = "Mifos X Open Banking",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .semantics { heading() },
            )

            Text(
                text = "Banking for Everyone",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                // TODO(a11y): honor a reduced-motion preference with a static fallback per
                //  ui.yaml#splash_loading_indicator.motion.reduced_motion_fallback once a
                //  cross-platform reduced-motion signal is available.
                modifier = Modifier
                    .padding(top = 32.dp)
                    .size(40.dp)
                    .semantics { contentDescription = "Loading, please wait" },
            )
        }
    }
}
