/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.product.FakeProductRepository
import org.mifosx.openbanking.feature.product.ProductFixtures
import org.mifosx.openbanking.feature.product.ProductRoute
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ProductViewModel]'s argument routing, the four states, error classification, the 404-as-empty
 * rule and retry.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the punctuation a prose-style name would carry.
 */
class ProductViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeProductRepository = FakeProductRepository(),
        accountId: String = ProductFixtures.ACCOUNT_ID,
    ): ProductViewModel = ProductViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(ProductViewModel.ACCOUNT_ID_ARG to accountId),
        ),
        repository = repository,
    )

    private fun content(vm: ProductViewModel): ProductUiState.Content =
        assertIs<ProductUiState.Content>(vm.stateFlow.value.uiState)

    // region — routing

    @Test
    fun accountIdIsReadFromSavedStateHandle() = runTest {
        val vm = viewModel(accountId = "acc-9")
        advanceUntilIdle()

        assertEquals("acc-9", vm.stateFlow.value.accountId)
    }

    @Test
    fun accountIdArgMatchesTheRoutePropertyName() {
        val descriptor = ProductRoute.serializer().descriptor

        assertEquals(1, descriptor.elementsCount)
        assertEquals(ProductViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(0))
    }

    @Test
    fun missingAccountIdYieldsBlankRatherThanCrashing() = runTest {
        val vm = ProductViewModel(SavedStateHandle(), FakeProductRepository())
        advanceUntilIdle()

        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun theAccountIdIsPassedToTheRepository() = runTest {
        val repository = FakeProductRepository()
        viewModel(repository, accountId = "acc-42")
        advanceUntilIdle()

        assertEquals("acc-42", repository.observedAccountId)
    }

    // region — lifecycle

    @Test
    fun theInitialStateIsLoading() {
        val state = ProductState()

        assertIs<ProductUiState.Loading>(state.uiState)
    }

    @Test
    fun mountingFetchesExactlyOnce() = runTest {
        val repository = FakeProductRepository()
        viewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    // region — content

    @Test
    fun aProductResolvesToContent() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val product = content(vm).product
        assertEquals("HSBC Advance Account", product.productName)
        assertEquals("PCA", product.productType)
        assertEquals(ProductFixtures.PRODUCT_ID, product.productId)
    }

    @Test
    fun theMonthlyChargeIsFormattedAsMoney() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("£0.00", content(vm).product.monthlyMaximumChargeLabel)
    }

    @Test
    fun aZeroChargeIsMarkedFeeFree() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(content(vm).product.isFeeFree)
    }

    @Test
    fun aNonZeroChargeIsNotMarkedFeeFree() = runTest {
        val repository = FakeProductRepository(
            NetworkResult.Success(ProductFixtures.terms().copy(monthlyMaximumCharge = "8.00")),
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        val product = content(vm).product
        assertEquals("£8.00", product.monthlyMaximumChargeLabel)
        assertEquals(false, product.isFeeFree)
    }

    @Test
    fun anAbsentChargeLeavesTheFeeLabelNull() = runTest {
        val repository = FakeProductRepository(
            NetworkResult.Success(ProductFixtures.terms().copy(monthlyMaximumCharge = null)),
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        assertNull(content(vm).product.monthlyMaximumChargeLabel)
    }

    @Test
    fun creditInterestTiersArePreservedInPayloadOrder() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val tiers = content(vm).product.creditInterestTiers
        assertEquals(2, tiers.size)
        assertEquals("£1,000", tiers[0].bandLimit)
        assertEquals("0.00", tiers[0].aer)
        assertEquals("Monthly", tiers[0].applicationFrequency)
        assertEquals("£10,000", tiers[1].bandLimit)
        assertEquals("0.15", tiers[1].aer)
    }

    @Test
    fun overdraftTiersArePreservedInPayloadOrder() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val tiers = content(vm).product.overdraftTiers
        assertEquals(2, tiers.size)
        assertEquals("Arranged", tiers[0].overdraftType)
        assertEquals("39.9", tiers[0].ear)
        assertEquals("Unarranged", tiers[1].overdraftType)
        assertEquals("49.9", tiers[1].ear)
    }

    @Test
    fun featuresArePassedThroughUnchanged() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(ProductFixtures.FEATURES, content(vm).product.features)
    }

    @Test
    fun aBusinessAccountRendersFromTheBcaBlock() = runTest {
        val repository = FakeProductRepository(
            NetworkResult.Success(ProductFixtures.businessTerms()),
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        val product = content(vm).product
        assertEquals("BCA", product.productType)
        assertEquals("HSBC Business Current Account", product.productName)
        assertEquals("£8.00", product.monthlyMaximumChargeLabel)
    }

    @Test
    fun aProductWithNoTiersStillResolvesToContent() = runTest {
        val repository = FakeProductRepository(
            NetworkResult.Success(
                ProductFixtures.terms().copy(
                    creditInterestTiers = emptyList(),
                    overdraftTiers = emptyList(),
                    features = emptyList(),
                ),
            ),
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        val product = content(vm).product
        assertTrue(product.creditInterestTiers.isEmpty())
        assertTrue(product.overdraftTiers.isEmpty())
        assertTrue(product.features.isEmpty())
    }

    // region — empty

    @Test
    fun anAbsentProductResolvesToEmpty() = runTest {
        val repository = FakeProductRepository(NetworkResult.Success(null))
        val vm = viewModel(repository)
        advanceUntilIdle()

        assertIs<ProductUiState.Empty>(vm.stateFlow.value.uiState)
    }

    /**
     * A 404 means the account has no product record, which is not a failure the customer can act on, so
     * it lands on the informational empty state rather than the error state.
     */
    @Test
    fun aNotFoundResolvesToEmptyRatherThanError() = runTest {
        val repository = FakeProductRepository(
            NetworkResult.Error(NetworkError.Client.NotFound()),
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        assertIs<ProductUiState.Empty>(vm.stateFlow.value.uiState)
    }

    // region — errors

    @Test
    fun anUnauthorizedResolvesToTokenExpired() = runTest {
        val vm = viewModel(FakeProductRepository(NetworkResult.Error(NetworkError.Client.Unauthorized())))
        advanceUntilIdle()

        val error = assertIs<ProductUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(ProductErrorKind.TokenExpired, error.kind)
    }

    @Test
    fun aForbiddenResolvesToConsentScope() = runTest {
        val vm = viewModel(FakeProductRepository(NetworkResult.Error(NetworkError.Client.Forbidden())))
        advanceUntilIdle()

        val error = assertIs<ProductUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(ProductErrorKind.ConsentScope, error.kind)
    }

    @Test
    fun aNetworkFailureResolvesToNetwork() = runTest {
        val vm = viewModel(
            FakeProductRepository(NetworkResult.Error(NetworkError.Network(RuntimeException("offline")))),
        )
        advanceUntilIdle()

        val error = assertIs<ProductUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(ProductErrorKind.Network, error.kind)
    }

    @Test
    fun anUnclassifiedFailureFallsBackToNetwork() = runTest {
        val vm = viewModel(FakeProductRepository(NetworkResult.Error(NetworkError.Client.RateLimited())))
        advanceUntilIdle()

        val error = assertIs<ProductUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(ProductErrorKind.Network, error.kind)
    }

    @Test
    fun classifyMapsUnauthorizedToTokenExpired() {
        assertEquals(
            ProductErrorKind.TokenExpired,
            classifyProductError(NetworkError.Client.Unauthorized()),
        )
    }

    @Test
    fun classifyMapsForbiddenToConsentScope() {
        assertEquals(
            ProductErrorKind.ConsentScope,
            classifyProductError(NetworkError.Client.Forbidden()),
        )
    }

    @Test
    fun classifyMapsServerFailuresToNetwork() {
        assertEquals(ProductErrorKind.Network, classifyProductError(NetworkError.Server(statusCode = 500)))
    }

    @Test
    fun classifyMapsSerializationFailuresToNetwork() {
        assertEquals(
            ProductErrorKind.Network,
            classifyProductError(NetworkError.Serialization(RuntimeException("bad body"))),
        )
    }

    // region — retry

    @Test
    fun retryRefetches() = runTest {
        val repository = FakeProductRepository(NetworkResult.Error(NetworkError.Network(RuntimeException("offline"))))
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.trySendAction(ProductAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }

    @Test
    fun retryRecoversFromErrorToContent() = runTest {
        val repository = FakeProductRepository(NetworkResult.Error(NetworkError.Network(RuntimeException("offline"))))
        val vm = viewModel(repository)
        advanceUntilIdle()
        assertIs<ProductUiState.Error>(vm.stateFlow.value.uiState)

        repository.result = NetworkResult.Success(ProductFixtures.terms())
        vm.trySendAction(ProductAction.RetryLoad)
        advanceUntilIdle()

        assertEquals("HSBC Advance Account", content(vm).product.productName)
    }

    @Test
    fun retryCanLandOnEmpty() = runTest {
        val repository = FakeProductRepository(NetworkResult.Error(NetworkError.Network(RuntimeException("offline"))))
        val vm = viewModel(repository)
        advanceUntilIdle()

        repository.result = NetworkResult.Success(null)
        vm.trySendAction(ProductAction.RetryLoad)
        advanceUntilIdle()

        assertIs<ProductUiState.Empty>(vm.stateFlow.value.uiState)
    }

    @Test
    fun anExplicitProductLoadAlsoRefetches() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.trySendAction(ProductAction.ProductLoad)
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }
}
