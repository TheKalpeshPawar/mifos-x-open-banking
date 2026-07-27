/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.android.app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Whether the OS itself will hand HSBC's callback to this app.
 *
 * That answer lives in the merged manifest and the package manager, not in Kotlin — a missing
 * `<data>` element or a wrong scheme compiles perfectly and simply never launches the app — so it is
 * the one part of the redirect path that genuinely needs a device.
 *
 * What happens *after* the intent arrives is [ConsentRedirectIntentTest] in `src/test`, on the JVM.
 * Keeping the two apart is deliberate: the previous single test needed `ActivityScenario`, which
 * cannot own a `singleTask` activity's lifecycle, and no amount of markers or timeouts fixed that —
 * it was asking a device to answer a question the device had nothing to do with.
 */
@RunWith(AndroidJUnit4::class)
class ConsentRedirectManifestTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun redirectIntent(url: String = REDIRECT_URL) =
        Intent(Intent.ACTION_VIEW, Uri.parse(url))

    @Test
    fun consentRedirectResolvesToMainActivity() {
        val resolved = context.packageManager.queryIntentActivities(redirectIntent(), 0)

        assertTrue(
            resolved.any { it.activityInfo.name == MainActivity::class.java.name },
            "no activity claims $REDIRECT_URL — the consent callback would open a browser, or " +
                "nothing at all, instead of returning to the app. Resolved: " +
                resolved.map { it.activityInfo.name },
        )
    }

    @Test
    fun theBridgePagesOauthHostAlsoResolves() {
        val oauthHost = "org.mifosx.openbanking://oauth/callback?code=abc"

        val resolved = context.packageManager.queryIntentActivities(redirectIntent(oauthHost), 0)

        assertTrue(
            resolved.any { it.activityInfo.name == MainActivity::class.java.name },
            "the bridge page is deployed separately from this app, so pinning one host means a " +
                "page revision silently stops the callback from ever reaching us",
        )
    }

    @Test
    fun mainActivityIsSingleTaskSoTheRedirectReusesTheRunningApp() {
        val activityInfo = context.packageManager
            .getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals(
            ActivityInfo.LAUNCH_SINGLE_TASK,
            activityInfo.launchMode,
            "without singleTask the redirect stacks a second MainActivity instead of reaching " +
                "onNewIntent on the instance that started the consent",
        )
    }

    @Test
    fun anUnrelatedSchemeDoesNotResolveToUs() {
        val resolved = context.packageManager
            .queryIntentActivities(redirectIntent("org.example.other://callback?code=x"), 0)

        assertTrue(
            resolved.none { it.activityInfo.name == MainActivity::class.java.name },
            "the intent-filter is broader than intended",
        )
    }

    private companion object {
        const val REDIRECT_URL = "org.mifosx.openbanking://callback?code=instrumented&state=st-1"
    }
}
