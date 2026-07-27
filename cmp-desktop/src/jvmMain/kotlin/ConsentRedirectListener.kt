/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */

import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.mifosx.openbanking.core.data.callback.ConsentRedirectBus
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Receives HSBC's consent redirect on desktop.
 *
 * Desktop has no custom-scheme handler to register against without platform-specific packaging, so
 * it uses the loopback listener that is the conventional desktop OAuth answer. HSBC still redirects
 * to its registered HTTPS callback page; that page rewrites the response from the URL fragment into
 * a query string and relays it here. The rewrite is necessary because a fragment is client-side only
 * and would never reach a server.
 *
 * Bound explicitly to loopback, so the listener is unreachable from the network.
 */
object ConsentRedirectListener {

    const val PORT = 8765
    private const val CALLBACK_PATH = "/callback"
    private const val HTTP_OK = 200
    private const val HTTP_NOT_FOUND = 404

    private val logger = Logger.withTag("ConsentRedirectListener")

    private var server: HttpServer? = null

    /**
     * Returns whether the listener is up. Idempotent: a second call while already listening is a
     * no-op.
     *
     * A failed bind stays non-fatal — the app must still start if another instance already holds the
     * port. It must not stay *silent*, though: the PSU authorises in the browser, the bridge page
     * relays to a dead port, and the app then waits forever on a callback that can never arrive,
     * with nothing anywhere saying why.
     */
    fun start(): Boolean {
        if (server != null) return true

        server = runCatching {
            HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 0).apply {
                createContext(CALLBACK_PATH, ::handleCallback)
                executor = null
                start()
            }
        }.onFailure { cause ->
            logger.e(cause) {
                "Could not bind the consent redirect listener to 127.0.0.1:$PORT — the consent " +
                    "journey cannot complete in this instance. Another copy of the app is the " +
                    "likely holder of the port; finish the consent there."
            }
        }.getOrNull()

        return server != null
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private fun handleCallback(exchange: HttpExchange) {
        val query = exchange.requestURI.rawQuery
        if (query.isNullOrBlank()) {
            exchange.respond(HTTP_NOT_FOUND, "No authorisation parameters were received.")
            return
        }

        ConsentRedirectBus.publish("$CALLBACK_SCHEME://callback?$query")
        exchange.respond(HTTP_OK, RETURN_TO_APP_PAGE)
    }

    private fun HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    private const val CALLBACK_SCHEME = "org.mifosx.openbanking"

    private val RETURN_TO_APP_PAGE = """
        <!doctype html>
        <html lang="en">
          <head><meta charset="utf-8"><title>Consent received</title></head>
          <body style="font-family: system-ui, sans-serif; text-align: center; padding: 3rem;">
            <h1>Consent received</h1>
            <p>You can close this tab and return to the app.</p>
          </body>
        </html>
    """.trimIndent()
}
