/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.mtls

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.io.ByteArrayInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Android mTLS: loads the PKCS#12 identity into a [KeyStore], builds an [SSLContext] whose key
 * managers present the transport client certificate, and installs it on the OkHttp engine. Server
 * trust uses the platform default trust store.
 */
actual fun HttpClientConfig<*>.installMtls(identity: MtlsIdentity) {
    val password = identity.pkcs12Password.toCharArray()

    val keyStore = KeyStore.getInstance("PKCS12").apply {
        ByteArrayInputStream(identity.pkcs12).use {
            this.load(it, password)
        }
    }
    val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        .apply { init(keyStore, password) }
        .keyManagers
    val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers
        .filterIsInstance<X509TrustManager>()
        .first()
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(keyManagers, arrayOf(trustManager), null)
    }

    @Suppress("UNCHECKED_CAST")
    (this as HttpClientConfig<OkHttpConfig>).engine {
        config {
            sslSocketFactory(sslContext.socketFactory, trustManager)
        }
    }
}
