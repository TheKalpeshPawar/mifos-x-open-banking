/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Splitting a refusal and an unreadable reply out of "connection failed".
 *
 * Both used to land on [PaymentConsentErrorKind.NetworkError], so the screen told the customer to
 * check their connection when the connection had worked perfectly, and offered a Retry that could
 * only fail again. A separate file rather than more cases in `PaymentConsentViewModelTest`, which
 * sits six lines under detekt's 600-line `LargeClass` cap.
 */
class PaymentConsentErrorClassificationTest {

    private fun remote(error: NetworkError): Throwable = RemoteException(error)

    private val obieBody = """
        {
          "Id": "ref-8891",
          "Errors": [
            {
              "ErrorCode": "U027",
              "Message": "Unsupported scheme",
              "Path": "Data.Initiation.CreditorAccount.SchemeName"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun aRefusalOnTheAuthorisationLegIsARejectionNotAConnectionFailure() {
        val kind = classifyPaymentConsentError(remote(NetworkError.Client.BadRequest(obieBody)))

        assertEquals(PaymentConsentErrorKind.RequestRejected, kind)
    }

    @Test
    fun anUnreadableReplyOnTheAuthorisationLegIsNotAConnectionFailure() {
        val kind = classifyPaymentConsentError(
            remote(NetworkError.Serialization(RuntimeException("bad json"))),
        )

        assertEquals(PaymentConsentErrorKind.ResponseUnreadable, kind)
    }

    /**
     * The assertion this whole change exists for.
     *
     * A `2xx` the app could not decode means the bank accepted the instruction and only the
     * identifier was lost, so the money may already have gone. Classifying it as
     * [PaymentConsentErrorKind.ResponseUnreadable] would attach a panel that promises nothing has
     * moved — the most expensive wrong answer this screen can give.
     */
    @Test
    fun anUnreadableReplyToTheSubmissionIsNotTheSameAsOneBeforeIt() {
        val decodeFailure = remote(NetworkError.Serialization(RuntimeException("bad json")))

        assertEquals(
            PaymentConsentErrorKind.ResponseUnreadable,
            classifyPaymentConsentError(decodeFailure),
        )
        assertEquals(
            PaymentConsentErrorKind.SubmissionUnconfirmed,
            classifySubmissionError(decodeFailure),
        )
    }

    @Test
    fun aRefusedSubmissionIsARejectionRatherThanTheCatchAll() {
        val kind = classifySubmissionError(remote(NetworkError.Client.BadRequest(obieBody)))

        assertEquals(PaymentConsentErrorKind.RequestRejected, kind)
    }

    /** A dropped connection during the submission still says nothing about whether money moved. */
    @Test
    fun aLostConnectionDuringSubmissionStaysTheCatchAll() {
        val kind = classifySubmissionError(
            remote(NetworkError.Network(cause = RuntimeException("offline"))),
        )

        assertEquals(PaymentConsentErrorKind.SubmissionFailed, kind)
    }

    @Test
    fun theBanksOwnCodeMessageAndReferenceAreKept() {
        val detail = errorDetailOf(remote(NetworkError.Client.BadRequest(obieBody)))

        assertNotNull(detail)
        assertEquals("U027", detail.code)
        assertEquals("Unsupported scheme", detail.message)
        assertEquals("ref-8891", detail.supportReference)
    }

    /**
     * A body the bank never filled in, or one a gateway mangled, must still classify as a rejection.
     *
     * The detail is what is missing, not the refusal — reading an unparseable body as "not really a
     * 400" would put the customer back on the connection-failure panel this change removes.
     */
    @Test
    fun anUnparseableBodyStillRejectsAndSimplyCarriesNoDetail() {
        val error = remote(NetworkError.Client.BadRequest("<html>gateway</html>"))

        assertEquals(PaymentConsentErrorKind.RequestRejected, classifyPaymentConsentError(error))
        assertNull(errorDetailOf(error))
    }

    /** Failures the app decided by itself have no bank response behind them. */
    @Test
    fun aFailureWithNoBankResponseCarriesNoDetail() {
        assertNull(errorDetailOf(RuntimeException("no remote call happened")))
    }

    /**
     * History records the bank's code and message alongside the app's own name for the failure.
     *
     * They answer different questions after the fact: the kind says what the app did, the code and
     * message say what the bank objected to. Only the first used to be written down.
     */
    @Test
    fun historyRecordsTheBanksAccountAsWellAsOurOwn() {
        val detail = PaymentConsentErrorDetail(
            message = "Unsupported scheme",
            code = "U027",
            supportReference = "ref-8891",
        )

        val recorded = PaymentConsentErrorKind.RequestRejected.description(detail)

        assertTrue("U027" in recorded, "the OBIE code is what makes a refusal diagnosable: $recorded")
        assertTrue("Unsupported scheme" in recorded, "the bank's own message was dropped: $recorded")
    }

    @Test
    fun historyStillReadsWhenTheBankSaidNothing() {
        val recorded = PaymentConsentErrorKind.ResponseUnreadable.description(detail = null)

        assertEquals(PaymentConsentErrorKind.ResponseUnreadable.description(), recorded)
    }
}
