package com.example.framebit.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestor de autenticación con Auth0.
 * Maneja login, logout y gestión de tokens.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val account: Auth0 by lazy {
        Auth0(
            context.getString(com.example.framebit.R.string.com_auth0_client_id),
            context.getString(com.example.framebit.R.string.com_auth0_domain)
        )
    }

    private val authenticationClient: AuthenticationAPIClient by lazy {
        AuthenticationAPIClient(account)
    }

    private var currentCredentials: Credentials? = null

    // Cache del perfil de Auth0 (sub, nombre, foto) tras login.
    private var cachedUserId: String? = null
    private var cachedName: String? = null
    private var cachedEmail: String? = null
    private var cachedPictureUrl: String? = null

    suspend fun login(activityContext: Context): Credentials = suspendCancellableCoroutine { continuation ->
        WebAuthProvider
            .login(account)
            .withScheme("com.example.moviebox")
            .withScope("openid profile email")
            .start(activityContext, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    currentCredentials = result
                    continuation.resume(result)
                }

                override fun onFailure(error: AuthenticationException) {
                    continuation.resumeWithException(error)
                }
            })
    }

    suspend fun logout(activityContext: Context): Unit = suspendCancellableCoroutine { continuation ->
        WebAuthProvider
            .logout(account)
            .withScheme("com.example.moviebox")
            .start(activityContext, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(result: Void?) {
                    currentCredentials = null
                    cachedUserId = null
                    cachedName = null
                    cachedEmail = null
                    cachedPictureUrl = null
                    continuation.resume(Unit)
                }

                override fun onFailure(error: AuthenticationException) {
                    continuation.resumeWithException(error)
                }
            })
    }

    suspend fun getUserProfile(accessToken: String): UserProfile =
        suspendCancellableCoroutine { continuation ->
            authenticationClient
                .userInfo(accessToken)
                .start(object : Callback<UserProfile, AuthenticationException> {
                    override fun onSuccess(result: UserProfile) {
                        // Rellenamos el cache para accesos posteriores sin hacer petición
                        cachedUserId = result.getId()
                        cachedName = result.name ?: result.nickname
                        cachedEmail = result.email
                        cachedPictureUrl = result.pictureURL
                        continuation.resume(result)
                    }

                    override fun onFailure(error: AuthenticationException) {
                        continuation.resumeWithException(error)
                    }
                })
        }

    fun isAuthenticated(): Boolean {
        return currentCredentials != null
    }

    fun getAccessToken(): String? {
        return currentCredentials?.accessToken
    }

    fun getCredentials(): Credentials? {
        return currentCredentials
    }

    // ===== Accesos rápidos al perfil cacheado (uso interno en ViewModels) =====

    fun getCachedUserId(): String? = cachedUserId
    fun getCachedName(): String? = cachedName
    fun getCachedPictureUrl(): String? = cachedPictureUrl


}
