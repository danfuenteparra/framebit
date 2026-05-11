package com.example.framebit.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestor de autenticación. Soporta dos vías:
 *  - Auth0 (Google etc.)  → userId = "auth0|..." / "google-oauth2|..."
 *  - Firebase email/password → userId = "firebase|<uid>"
 *
 * Internamente, ambas dejan una sesión Firebase Auth activa para que las reglas
 * de Firestore pasen. La diferencia es que con email/password no es anónima.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
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

    // Cache del perfil del usuario actual (sirve para Auth0 y para email/password).
    private var cachedUserId: String? = null
    private var cachedName: String? = null
    private var cachedEmail: String? = null
    private var cachedPictureUrl: String? = null

    /** True si el usuario entró por email/password en vez de por Auth0. */
    private var isEmailSession: Boolean = false

    // ===================== AUTH0 =====================

    suspend fun login(activityContext: Context): Credentials = suspendCancellableCoroutine { continuation ->
        WebAuthProvider
            .login(account)
            .withScheme("com.example.moviebox")
            .withScope("openid profile email")
            .start(activityContext, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    currentCredentials = result
                    isEmailSession = false
                    continuation.resume(result)
                }

                override fun onFailure(error: AuthenticationException) {
                    continuation.resumeWithException(error)
                }
            })
    }

    suspend fun logout(activityContext: Context) {
        // Si la sesión es de email/password, no hay flujo web de Auth0 que cerrar.
        if (isEmailSession) {
            firebaseAuth.signOut()
            clearCache()
            return
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            WebAuthProvider
                .logout(account)
                .withScheme("com.example.moviebox")
                .start(activityContext, object : Callback<Void?, AuthenticationException> {
                    override fun onSuccess(result: Void?) {
                        firebaseAuth.signOut()
                        clearCache()
                        continuation.resume(Unit)
                    }

                    override fun onFailure(error: AuthenticationException) {
                        continuation.resumeWithException(error)
                    }
                })
        }
    }

    suspend fun getUserProfile(accessToken: String): UserProfile =
        suspendCancellableCoroutine { continuation ->
            authenticationClient
                .userInfo(accessToken)
                .start(object : Callback<UserProfile, AuthenticationException> {
                    override fun onSuccess(result: UserProfile) {
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

    fun isAuthenticated(): Boolean = currentCredentials != null || isEmailSession

    fun getAccessToken(): String? = currentCredentials?.accessToken
    fun getCredentials(): Credentials? = currentCredentials

    // ===================== FIREBASE EMAIL / PASSWORD =====================

    /**
     * Login con email y contraseña usando Firebase Auth.
     * Tras autenticarse, rellena el cache con uid/nombre/email.
     */
    suspend fun loginWithEmail(email: String, password: String) {
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("No se obtuvo usuario tras login")
        cachedUserId = "firebase|${user.uid}"
        cachedName = user.displayName?.ifBlank { user.email?.substringBefore("@") } ?: user.email?.substringBefore("@") ?: "Usuario"
        cachedEmail = user.email
        cachedPictureUrl = user.photoUrl?.toString()
        isEmailSession = true
    }

    /**
     * Registra un usuario nuevo con email/password.
     * Si [displayName] no es vacío, lo asigna como displayName del perfil de Firebase.
     */
    suspend fun registerWithEmail(email: String, password: String, displayName: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("No se obtuvo usuario tras registro")
        if (displayName.isNotBlank()) {
            val update = userProfileChangeRequest { this.displayName = displayName }
            user.updateProfile(update).await()
        }
        cachedUserId = "firebase|${user.uid}"
        cachedName = displayName.ifBlank { user.email?.substringBefore("@") ?: "Usuario" }
        cachedEmail = user.email
        cachedPictureUrl = null
        isEmailSession = true
    }

    private fun clearCache() {
        currentCredentials = null
        cachedUserId = null
        cachedName = null
        cachedEmail = null
        cachedPictureUrl = null
        isEmailSession = false
    }

    // ===== Accesos rápidos al perfil cacheado =====

    fun getCachedUserId(): String? = cachedUserId
    fun getCachedName(): String? = cachedName
    fun getCachedEmail(): String? = cachedEmail
    fun getCachedPictureUrl(): String? = cachedPictureUrl

    fun isEmailSession(): Boolean = isEmailSession
}