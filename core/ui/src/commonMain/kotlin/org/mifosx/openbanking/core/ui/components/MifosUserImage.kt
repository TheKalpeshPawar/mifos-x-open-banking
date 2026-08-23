/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme

/** A circular profile image, or a single-letter avatar when no bitmap is supplied. */
@Composable
fun MifosUserImage(
    bitmap: ByteArray?,
    modifier: Modifier = Modifier,
    username: String? = null,
) {
    val context = LocalPlatformContext.current

    if (bitmap == null) {
        MifosTextUserImage(
            text = username?.firstOrNull()?.toString() ?: "M",
            modifier = modifier,
        )
    } else {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(bitmap)
                .build(),
            imageLoader = ImageLoader(context),
        )
        Image(
            modifier = modifier
                .clip(CircleShape)
                .fillMaxSize(),
            painter = painter,
            contentDescription = "Profile Image",
            contentScale = ContentScale.Crop,
        )
    }
}

@Preview
@Composable
private fun MifosUserImagePreview() {
    MifosXOpenBankingTheme {
        MifosUserImage(bitmap = null, username = "John Doe")
    }
}
