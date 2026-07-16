/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login

import org.mifosx.openbanking.core.data.login.ConsentResult
import org.mifosx.openbanking.core.data.login.LoginRepository
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.feature.login.browser.BrowserLauncher
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

class FakeLoginRepository : LoginRepository {
    @Suppress("ktlint:standard:property-naming")
    private var _permissions: List<OBPermission> = OBPermission.ALL
    var createConsentResult: NetworkResult<ConsentResult, NetworkError> = NetworkResult.Success(
        ConsentResult(
            authorizationUrl = "https://sandbox.test/authorize",
            state = "test-state",
            nonce = "test-nonce",
            consentId = "test-consent-id",
        ),
    )
    var createConsentCallCount = 0
        private set

    override fun getPermissions(): List<OBPermission> = _permissions

    fun setPermissions(value: List<OBPermission>) {
        _permissions = value
    }

    override suspend fun createConsentAndBuildAuthorizationUrl(): NetworkResult<ConsentResult, NetworkError> {
        createConsentCallCount++
        return createConsentResult
    }
}

class FakeBrowserLauncher : BrowserLauncher {
    var launchedUrl: String? = null
        private set

    override fun launch(url: String) {
        launchedUrl = url
    }
}
