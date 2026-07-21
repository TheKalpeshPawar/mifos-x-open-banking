/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission

/**
 * Builds one row per granted permission, in the order the consent catalogue declares them.
 *
 * The whole scope is listed rather than a headline subset: HSBC grants the consent all-or-nothing,
 * so an account authorised at all carries every requested permission, and a user auditing what they
 * agreed to is owed the full list rather than a curated four. The card keeps it out of the way
 * behind an expandable toggle instead of trimming it.
 *
 * Kept free of any Compose reference so the mapping is unit-testable on the JVM without a renderer,
 * and read from [OBPermission.ALL] — the requested scope is declared there once, with its display
 * labels, and a permission added to or dropped from it flows through here without a second edit.
 */
internal fun profilePermissions(
    granted: List<OBPermission> = OBPermission.ALL,
): List<ProfilePermissionUi> =
    granted.map { permission -> ProfilePermissionUi(id = permission.id, label = permission.label) }
