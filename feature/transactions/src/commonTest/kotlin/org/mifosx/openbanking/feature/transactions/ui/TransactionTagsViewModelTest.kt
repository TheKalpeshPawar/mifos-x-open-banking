/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.transactions.TransactionMetadataRepository
import org.mifosx.openbanking.core.data.transactions.TransactionMetadataSnapshot
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionComment
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.model.obp.TransactionTag
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun tagsTxn() = Transaction(
    id = "tx-1",
    otherAccount = TransactionCounterparty(holder = CounterpartyHolder(name = "Whole Foods Market")),
    details = TransactionDetails(
        description = "Weekly shop",
        completed = "2026-05-23T14:32:00Z",
        value = AmountOfMoney(currency = "GBP", amount = "-67.84"),
    ),
)

private class TagsFakeTransactionsRepository(
    var result: Result<Transaction> = Result.success(tagsTxn()),
) : TransactionsRepository {
    override fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> = TODO()
    override suspend fun listTransactions(bankId: String, accountId: String, limit: Int?) = TODO()
    override suspend fun listTransactionsWithAttributes(bankId: String, accountId: String, limit: Int?) = TODO()
    override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) = result
}

private class TagsFakeMetadataRepository(
    var snapshot: Result<TransactionMetadataSnapshot> = Result.success(
        TransactionMetadataSnapshot(
            tags = listOf(TransactionTag(id = "t1", value = "#groceries")),
            note = TransactionComment(id = "c1", value = "Weekly shop note", date = "2026-06-01T00:00:00Z"),
        ),
    ),
    var addResult: (String) -> Result<TransactionTag> = { Result.success(TransactionTag(id = "t-new", value = it)) },
    var removeResult: Result<Unit> = Result.success(Unit),
    var noteResult: Result<TransactionComment?> = Result.success(TransactionComment(id = "c-new")),
) : TransactionMetadataRepository {
    var added = mutableListOf<String>()
    var removed = mutableListOf<String>()
    var replacedNote: String? = null

    override suspend fun metadata(bankId: String, accountId: String, transactionId: String) = snapshot

    override suspend fun addTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        value: String,
    ): Result<TransactionTag> {
        added += value
        return addResult(value)
    }

    override suspend fun removeTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        tagId: String,
    ): Result<Unit> {
        removed += tagId
        return removeResult
    }

    override suspend fun replaceNote(
        bankId: String,
        accountId: String,
        transactionId: String,
        text: String,
    ): Result<TransactionComment?> {
        replacedNote = text
        return noteResult
    }
}

class TransactionTagsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        metadata: TagsFakeMetadataRepository = TagsFakeMetadataRepository(),
        transactions: TagsFakeTransactionsRepository = TagsFakeTransactionsRepository(),
    ) = TransactionTagsViewModel(
        metadataRepository = metadata,
        transactionsRepository = transactions,
        bankId = "ac.bank.uk",
        accountId = "ac.checking.001",
        transactionId = "tx-1",
    )

    private suspend fun TestScope.content(model: TransactionTagsViewModel): TransactionTagsContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_derivesHeaderAndMetadata() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals("Whole Foods Market", c.merchant)
        assertEquals("-£67.84", c.amount)
        assertEquals("23 May 2026, 14:32", c.dateTime)
        assertEquals(listOf("#groceries"), c.tags.map { it.value })
        assertEquals("Weekly shop note", c.note)
        assertEquals("", c.tagInput)
        assertFalse(c.isSaving)
    }

    @Test
    fun addTag_normalizesAppendsAndClearsInput() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onTagInputChanged("holiday")
        model.onAddTag()
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("#groceries", "#holiday"), c.tags.map { it.value })
        assertEquals("", c.tagInput)
        assertEquals(listOf("#holiday"), metadata.added)
    }

    @Test
    fun addTag_blankShowsNoticeWithoutApiCall() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onTagInputChanged("   ")
        model.onAddTag()
        advanceUntilIdle()
        assertNotNull(model.notice.value)
        assertTrue(metadata.added.isEmpty())
    }

    @Test
    fun addTag_duplicateShowsNoticeWithoutApiCall() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onTagInputChanged("#GROCERIES")
        model.onAddTag()
        advanceUntilIdle()
        assertNotNull(model.notice.value)
        assertTrue(metadata.added.isEmpty())
    }

    @Test
    fun removeTag_removesChip() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onRemoveTag("t1")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertTrue(c.tags.isEmpty())
        assertEquals(listOf("t1"), metadata.removed)
    }

    @Test
    fun save_replacesNoteAndSetsSaved() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onNoteChanged("new note")
        model.onSave()
        advanceUntilIdle()
        assertEquals("new note", metadata.replacedNote)
        assertTrue(model.saved.value)
    }

    @Test
    fun save_failureShowsNoticeAndStays() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository(noteResult = Result.failure(RuntimeException("boom")))
        val model = vm(metadata)
        content(model)
        model.onSave()
        advanceUntilIdle()
        assertFalse(model.saved.value)
        assertNotNull(model.notice.value)
        assertFalse((model.uiState.value as ScreenState.Content).data.isSaving)
    }

    @Test
    fun noticeConsumed_clearsNotice() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onTagInputChanged(" ")
        model.onAddTag()
        advanceUntilIdle()
        assertNotNull(model.notice.value)
        model.onNoticeConsumed()
        assertNull(model.notice.value)
    }

    @Test
    fun load_suggestionsExcludeExistingTags() = runTest(dispatcher) {
        val c = content(vm())
        assertFalse("#groceries" in c.suggestions)
        assertTrue("#rent" in c.suggestions)
    }

    @Test
    fun suggestionClick_addsChipAndRemovesFromSuggestions() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository()
        val model = vm(metadata)
        content(model)
        model.onSuggestionClick("#rent")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertTrue(c.tags.any { it.value == "#rent" })
        assertFalse("#rent" in c.suggestions)
        assertEquals(listOf("#rent"), metadata.added)
    }

    @Test
    fun removeTag_restoresSuggestion() = runTest(dispatcher) {
        val model = vm()
        val before = content(model)
        assertFalse("#groceries" in before.suggestions)
        model.onRemoveTag("t1")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertTrue("#groceries" in c.suggestions)
    }

    @Test
    fun load_metadataFailureIsErrorThenRetryRecovers() = runTest(dispatcher) {
        val metadata = TagsFakeMetadataRepository(snapshot = Result.failure(RuntimeException("boom")))
        val model = vm(metadata)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        metadata.snapshot = Result.success(TransactionMetadataSnapshot())
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
    }
}
