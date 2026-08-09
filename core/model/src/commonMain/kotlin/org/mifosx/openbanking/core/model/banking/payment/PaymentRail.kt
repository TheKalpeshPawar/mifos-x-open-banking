/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

/**
 * Which payment rail the form is operating on. Drives field visibility (CurrencyOfTransfer,
 * ChargeBearer on international; RemittanceInformation on domestic) and beneficiary filtering
 * (SortCode vs IBAN).
 *
 * **Not the endpoint discriminator**, though it once claimed to be. A path is chosen by product and
 * rail together — `domestic-payment-consents` and `domestic-standing-order-consents` share this
 * value and are different endpoints — so dispatch is written against [ConsentType]. Reading this as
 * "which endpoint" is what let an international consent be read back at the domestic path.
 */
sealed interface PaymentRail {
    data object Domestic : PaymentRail
    data object International : PaymentRail
}
