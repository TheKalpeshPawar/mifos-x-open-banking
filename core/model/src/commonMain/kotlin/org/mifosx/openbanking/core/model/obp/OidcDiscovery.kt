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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OBP `GET /obp/vX/well-known` response — the list of OIDC providers OBP trusts. */
@Serializable
data class OidcProviderList(
    @SerialName("well_known_uris") val wellKnownUris: List<OidcProviderRef> = emptyList(),
)

/** A single OIDC provider entry: its [provider] id and its discovery-document [url]. */
@Serializable
data class OidcProviderRef(
    val provider: String = "",
    val url: String = "",
)

/**
 * OpenID Connect discovery document (`.well-known/openid-configuration`). Only the fields the
 * app drives the authorization-code flow with are modelled; unknown keys are ignored.
 */
@Serializable
data class OidcConfiguration(
    val issuer: String = "",
    @SerialName("authorization_endpoint") val authorizationEndpoint: String = "",
    @SerialName("token_endpoint") val tokenEndpoint: String = "",
    @SerialName("userinfo_endpoint") val userinfoEndpoint: String = "",
    @SerialName("jwks_uri") val jwksUri: String = "",
    @SerialName("registration_endpoint") val registrationEndpoint: String = "",
    @SerialName("scopes_supported") val scopesSupported: List<String> = emptyList(),
    @SerialName("code_challenge_methods_supported") val codeChallengeMethodsSupported: List<String> = emptyList(),
)

/**
 * RFC 7591 dynamic client-registration request for a native public client. The custom-scheme
 * [redirectUris] are whitelisted by the provider so its authorization endpoint accepts our
 * callback; [tokenEndpointAuthMethod] is `none` because a mobile app holds no client secret.
 */
@Serializable
data class OidcClientRegistrationRequest(
    @SerialName("client_name") val clientName: String,
    @SerialName("redirect_uris") val redirectUris: List<String>,
    @SerialName("token_endpoint_auth_method") val tokenEndpointAuthMethod: String = "none",
    @SerialName("grant_types") val grantTypes: List<String> = listOf("authorization_code", "refresh_token"),
    @SerialName("response_types") val responseTypes: List<String> = listOf("code"),
    val scope: String = "openid profile email",
    @SerialName("application_type") val applicationType: String = "native",
)

/** RFC 7591 registration response — the issued [clientId] identifies the app at the OIDC provider. */
@Serializable
data class OidcClientRegistrationResponse(
    @SerialName("client_id") val clientId: String = "",
    @SerialName("redirect_uris") val redirectUris: List<String> = emptyList(),
)
