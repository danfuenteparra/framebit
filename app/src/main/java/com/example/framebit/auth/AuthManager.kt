package com.example.framebit.auth

import android.content.Context
import android.content.SharedPreferences
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
 * Persiste la sesión en SharedPreferences para que al abrir la app no haya
 * que volver a hacer login (incluso sin conexión).
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("framebit_auth", Context.MODE_PRIVATE)

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

    // Cache en memoria (se rellena desde SharedPreferences al arrancar)
    private var cachedUserId: String? = null
    private var cachedName: String? = null
    private var cachedEmail: String? = null
    private var cachedPictureUrl: String? = null
    private var isEmailSession: Boolean = false

    init {
        // Restaurar sesión persistida al construir la clase
        restoreSession()
    }

    private fun restoreSession() {
        cachedUserId = prefs.getString(KEY_USER_ID, null)
        cachedName = prefs.getString(KEY_NAME, null)
        cachedEmail = prefs.getString(KEY_EMAIL, null)
        cachedPictureUrl = prefs.getString(KEY_PICTURE, null)
        isEmailSession = prefs.getBoolean(KEY_IS_EMAIL, false)
    }

    private fun persistSession() {
        prefs.edit()
            .putString(KEY_USER_ID, cachedUserId)
            .putString(KEY_NAME, cachedName)
            .putString(KEY_EMAIL, cachedEmail)
            .putString(KEY_PICTURE, cachedPictureUrl)
            .putBoolean(KEY_IS_EMAIL, isEmailSession)
            .apply()
    }

    private fun clearPersistedSession() {
        prefs.edit().clear().apply()
    }

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
                        persistSession()  // <-- guardar tras login Auth0
                        continuation.resume(result)
                    }
                    override fun onFailure(error: AuthenticationException) {
                        continuation.resumeWithException(error)
                    }
                })
        }

    fun isAuthenticated(): Boolean =
        currentCredentials != null || isEmailSession || !cachedUserId.isNullOrBlank()

    fun getAccessToken(): String? = currentCredentials?.accessToken
    fun getCredentials(): Credentials? = currentCredentials

    // ===================== FIREBASE EMAIL / PASSWORD =====================

    suspend fun loginWithEmail(email: String, password: String) {
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("No se obtuvo usuario tras login")
        cachedUserId = "firebase|${user.uid}"
        cachedName = user.displayName?.ifBlank { user.email?.substringBefore("@") }
            ?: user.email?.substringBefore("@") ?: "Usuario"
        cachedEmail = user.email
        cachedPictureUrl = user.photoUrl?.toString()
        isEmailSession = true
        persistSession()
    }

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
        persistSession()
    }

    private fun clearCache() {
        currentCredentials = null
        cachedUserId = null
        cachedName = null
        cachedEmail = null
        cachedPictureUrl = null
        isEmailSession = false
        clearPersistedSession()
    }

    // ===== Accesos rápidos al perfil cacheado =====

    fun getCachedUserId(): String? = cachedUserId
    fun getCachedName(): String? = cachedName
    fun getCachedEmail(): String? = cachedEmail
    fun getCachedPictureUrl(): String? = cachedPictureUrl

    fun isEmailSession(): Boolean = isEmailSession

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PICTURE = "picture"
        private const val KEY_IS_EMAIL = "is_email_session"
    }
}