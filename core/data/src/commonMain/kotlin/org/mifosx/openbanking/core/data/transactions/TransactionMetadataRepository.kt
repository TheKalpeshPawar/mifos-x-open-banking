/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.transactions

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.MetadataValueRequest
import org.mifosx.openbanking.core.model.obp.TransactionComment
import org.mifosx.openbanking.core.model.obp.TransactionTag
import org.mifosx.openbanking.core.network.api.TransactionMetadataApi

/** A transaction's user metadata: its tags plus the newest comment, surfaced as the note. */
data class TransactionMetadataSnapshot(
    val tags: List<TransactionTag> = emptyList(),
    val note: TransactionComment? = null,
)

/**
 * User metadata (tags + private note) on one transaction, via the OBP v1.2.1
 * transaction-metadata endpoints. The note is modelled as the newest comment;
 * [replaceNote] deletes existing comments before posting so the note stays canonical
 * instead of accumulating an append-only comment pile.
 */
interface TransactionMetadataRepository {
    suspend fun metadata(bankId: String, accountId: String, transactionId: String): Result<TransactionMetadataSnapshot>

    suspend fun addTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        value: String,
    ): Result<TransactionTag>

    suspend fun removeTag(bankId: String, accountId: String, transactionId: String, tagId: String): Result<Unit>

    /** Replaces the note: deletes existing comments, then posts [text] (blank = delete only). */
    suspend fun replaceNote(
        bankId: String,
        accountId: String,
        transactionId: String,
        text: String,
    ): Result<TransactionComment?>
}

class TransactionMetadataRepositoryImpl(
    private val api: TransactionMetadataApi,
) : TransactionMetadataRepository {

    override suspend fun metadata(
        bankId: String,
        accountId: String,
        transactionId: String,
    ): Result<TransactionMetadataSnapshot> = runCatching {
        val tags = api.listTags(bankId, accountId, transactionId).toResult().getOrThrow().tags
        val comments = api.listComments(bankId, accountId, transactionId).toResult().getOrThrow().comments
        TransactionMetadataSnapshot(
            tags = tags,
            note = comments.maxByOrNull { it.date },
        )
    }

    override suspend fun addTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        value: String,
    ): Result<TransactionTag> =
        api.addTag(bankId, accountId, transactionId, MetadataValueRequest(value)).toResult()

    override suspend fun removeTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        tagId: String,
    ): Result<Unit> =
        api.deleteTag(bankId, accountId, transactionId, tagId).toResult()

    override suspend fun replaceNote(
        bankId: String,
        accountId: String,
        transactionId: String,
        text: String,
    ): Result<TransactionComment?> = runCatching {
        val existing = api.listComments(bankId, accountId, transactionId).toResult().getOrThrow().comments
        existing.forEach { comment ->
            api.deleteComment(bankId, accountId, transactionId, comment.id).toResult().getOrThrow()
        }
        if (text.isBlank()) {
            null
        } else {
            api.addComment(bankId, accountId, transactionId, MetadataValueRequest(text)).toResult().getOrThrow()
        }
    }
}
