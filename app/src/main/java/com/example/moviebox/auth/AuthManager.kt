package com.example.moviebox.auth

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
 * Gestor de autenticación con Auth0
 * Maneja login, logout y gestión de tokens
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Cliente de Auth0
    private val account: Auth0 by lazy {
        Auth0(
            context.getString(com.example.moviebox.R.string.com_auth0_client_id),
            context.getString(com.example.moviebox.R.string.com_auth0_domain)
        )
    }

    // Cliente de API de autenticación
    private val authenticationClient: AuthenticationAPIClient by lazy {
        AuthenticationAPIClient(account)
    }

    // Credenciales actuales del usuario
    private var currentCredentials: Credentials? = null

    /**
     * Inicia el proceso de login con Auth0
     * Abre el navegador para autenticación
     */
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

    /**
     * Cierra la sesión del usuario
     */
    suspend fun logout(activityContext: Context): Unit = suspendCancellableCoroutine { continuation ->
        WebAuthProvider
            .logout(account)
            .withScheme("com.example.moviebox")
            .start(activityContext, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(result: Void?) {
                    currentCredentials = null
                    continuation.resume(Unit)
                }

                override fun onFailure(error: AuthenticationException) {
                    continuation.resumeWithException(error)
                }
            })
    }

    /**
     * Obtiene el perfil del usuario autenticado
     */
    suspend fun getUserProfile(accessToken: String): UserProfile =
        suspendCancellableCoroutine { continuation ->
            authenticationClient
                .userInfo(accessToken)
                .start(object : Callback<UserProfile, AuthenticationException> {
                    override fun onSuccess(result: UserProfile) {
                        continuation.resume(result)
                    }

                    override fun onFailure(error: AuthenticationException) {
                        continuation.resumeWithException(error)
                    }
                })
        }

    /**
     * Verifica si hay un usuario autenticado
     */
    fun isAuthenticated(): Boolean {
        return currentCredentials != null
    }

    /**
     * Obtiene el token de acceso actual
     */
    fun getAccessToken(): String? {
        return currentCredentials?.accessToken
    }

    /**
     * Obtiene las credenciales actuales
     */
    fun getCredentials(): Credentials? {
        return currentCredentials
    }
}