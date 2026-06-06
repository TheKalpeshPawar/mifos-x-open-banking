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

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.MetadataValueRequest
import org.mifosx.openbanking.core.model.obp.TransactionComment
import org.mifosx.openbanking.core.model.obp.TransactionCommentsResponse
import org.mifosx.openbanking.core.model.obp.TransactionTag
import org.mifosx.openbanking.core.model.obp.TransactionTagsResponse
import org.mifosx.openbanking.core.network.api.TransactionMetadataApi
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeMetadataApi(
    var tags: NetworkResult<TransactionTagsResponse, NetworkError> =
        NetworkResult.Success(TransactionTagsResponse()),
    var comments: NetworkResult<TransactionCommentsResponse, NetworkError> =
        NetworkResult.Success(TransactionCommentsResponse()),
) : TransactionMetadataApi {
    val deletedComments = mutableListOf<String>()
    val deletedTags = mutableListOf<String>()
    var addedComment: String? = null

    override suspend fun listTags(bankId: String, accountId: String, transactionId: String) = tags

    override suspend fun addTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        request: MetadataValueRequest,
    ): NetworkResult<TransactionTag, NetworkError> =
        NetworkResult.Success(TransactionTag(id = "tag-new", value = request.value))

    override suspend fun deleteTag(
        bankId: String,
        accountId: String,
        transactionId: String,
        tagId: String,
    ): NetworkResult<Unit, NetworkError> {
        deletedTags += tagId
        return NetworkResult.Success(Unit)
    }

    override suspend fun listComments(bankId: String, accountId: String, transactionId: String) = comments

    override suspend fun addComment(
        bankId: String,
        accountId: String,
        transactionId: String,
        request: MetadataValueRequest,
    ): NetworkResult<TransactionComment, NetworkError> {
        addedComment = request.value
        return NetworkResult.Success(TransactionComment(id = "c-new", value = request.value))
    }

    override suspend fun deleteComment(
        bankId: String,
        accountId: String,
        transactionId: String,
        commentId: String,
    ): NetworkResult<Unit, NetworkError> {
        deletedComments += commentId
        return NetworkResult.Success(Unit)
    }
}

class TransactionMetadataRepositoryTest {

    private fun repository(api: FakeMetadataApi) = TransactionMetadataRepositoryImpl(api)

    @Test
    fun metadataReturnsTagsAndNewestCommentAsNote() = runTest {
        val api = FakeMetadataApi(
            tags = NetworkResult.Success(
                TransactionTagsResponse(listOf(TransactionTag(id = "t1", value = "#groceries"))),
            ),
            comments = NetworkResult.Success(
                TransactionCommentsResponse(
                    listOf(
                        TransactionComment(id = "c1", value = "old", date = "2026-06-01T00:00:00Z"),
                        TransactionComment(id = "c2", value = "new", date = "2026-06-05T00:00:00Z"),
                    ),
                ),
            ),
        )
        val snapshot = repository(api).metadata("b", "a", "t").getOrThrow()
        assertEquals(listOf("#groceries"), snapshot.tags.map { it.value })
        assertEquals("new", snapshot.note?.value)
    }

    @Test
    fun metadataFailsWhenTagsCallFails() = runTest {
        val api = FakeMetadataApi(tags = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        assertTrue(repository(api).metadata("b", "a", "t").isFailure)
    }

    @Test
    fun replaceNoteDeletesExistingThenPosts() = runTest {
        val api = FakeMetadataApi(
            comments = NetworkResult.Success(
                TransactionCommentsResponse(
                    listOf(
                        TransactionComment(id = "c1", value = "old1"),
                        TransactionComment(id = "c2", value = "old2"),
                    ),
                ),
            ),
        )
        val note = repository(api).replaceNote("b", "a", "t", "fresh note").getOrThrow()
        assertEquals(listOf("c1", "c2"), api.deletedComments)
        assertEquals("fresh note", api.addedComment)
        assertEquals("fresh note", note?.value)
    }

    @Test
    fun replaceNoteWithBlankTextOnlyDeletes() = runTest {
        val api = FakeMetadataApi(
            comments = NetworkResult.Success(
                TransactionCommentsResponse(listOf(TransactionComment(id = "c1", value = "old"))),
            ),
        )
        val note = repository(api).replaceNote("b", "a", "t", "  ").getOrThrow()
        assertEquals(listOf("c1"), api.deletedComments)
        assertNull(api.addedComment)
        assertNull(note)
    }

    @Test
    fun removeTagDelegatesToApi() = runTest {
        val api = FakeMetadataApi()
        repository(api).removeTag("b", "a", "t", "tag-9").getOrThrow()
        assertEquals(listOf("tag-9"), api.deletedTags)
    }

    @Test
    fun addTagReturnsCreatedTag() = runTest {
        val tag = repository(FakeMetadataApi()).addTag("b", "a", "t", "#holiday").getOrThrow()
        assertEquals("#holiday", tag.value)
        assertEquals("tag-new", tag.id)
    }
}
