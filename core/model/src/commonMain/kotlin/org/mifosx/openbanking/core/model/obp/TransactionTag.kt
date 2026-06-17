/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.Serializable

/** A user tag on a transaction (OBP v1.2.1 transaction metadata). */
@Serializable
data class TransactionTag(
    val id: String = "",
    /** Tag text, conventionally "#groceries"-style. */
    val value: String = "",
    val date: String = "",
)

/** Wrapper for `GET .../metadata/tags` (`{ "tags": [...] }`). */
@Serializable
data class TransactionTagsResponse(
    val tags: List<TransactionTag> = emptyList(),
)

/** A user comment on a transaction — the app surfaces the newest one as the private note. */
@Serializable
data class TransactionComment(
    val id: String = "",
    val value: String = "",
    val date: String = "",
)

/** Wrapper for `GET .../metadata/comments` (`{ "comments": [...] }`). */
@Serializable
data class TransactionCommentsResponse(
    val comments: List<TransactionComment> = emptyList(),
)

/** Request body for tag and comment creation (both take `{ "value": "..." }`). */
@Serializable
data class MetadataValueRequest(
    val value: String,
)
