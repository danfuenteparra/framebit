package com.example.moviebox.data.remote.firebase

import com.example.moviebox.data.remote.model.BlockRelation
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.remote.model.LibraryEntry
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.remote.model.ReviewComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {

    // =================== AUTH BRIDGE ===================

    suspend fun ensureFirebaseSignedIn() {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
    }

    // =================== USUARIOS ===================

    suspend fun upsertCurrentUser(
        userId: String,
        name: String,
        email: String,
        pictureUrl: String?
    ) {
        val ref = firestore.collection("users").document(userId)
        val existing = ref.get().await()

        // Preservamos campos editables que ya tuviera el usuario (bio, links, foto custom)
        val preservedPicture = if (existing.exists() && existing.getString("pictureUrl") != null)
            existing.getString("pictureUrl")
        else pictureUrl

        val data = mapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "pictureUrl" to preservedPicture,
            "searchableName" to name.lowercase(),
            "followersCount" to (existing.getLong("followersCount") ?: 0L),
            "followingCount" to (existing.getLong("followingCount") ?: 0L),
            "bio" to (existing.getString("bio") ?: ""),
            "links" to (existing.get("links") as? List<*> ?: emptyList<String>())
        )
        ref.set(data).await()
    }

    /** Actualiza datos editables del perfil: bio, links y/o foto. */
    suspend fun updateUserProfile(
        userId: String,
        bio: String?,
        links: List<String>?,
        pictureUrl: String?
    ) {
        val updates = mutableMapOf<String, Any?>()
        if (bio != null) updates["bio"] = bio
        if (links != null) updates["links"] = links
        if (pictureUrl != null) updates["pictureUrl"] = pictureUrl
        if (updates.isEmpty()) return
        firestore.collection("users").document(userId)
            .set(updates, SetOptions.merge()).await()
    }

    suspend fun getUser(userId: String): PublicUser? {
        return try {
            firestore.collection("users").document(userId)
                .get().await()
                .toObject(PublicUser::class.java)
        } catch (e: Exception) { null }
    }

    /**
     * Búsqueda de usuarios por prefijo de nombre.
     * Excluye al usuario actual y a cualquiera con bloqueo activo en cualquier dirección.
     */
    suspend fun searchUsers(query: String, excludeUserId: String): List<PublicUser> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return try {
            val raw = firestore.collection("users")
                .orderBy("searchableName")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(PublicUser::class.java)

            // Calculamos los IDs ocultos (yo bloqueé + me bloquearon) para filtrar
            val hidden = if (excludeUserId.isBlank()) emptySet()
            else getMutuallyHiddenIds(excludeUserId)

            raw.filter { it.userId != excludeUserId && it.userId !in hidden }
        } catch (e: Exception) { emptyList() }
    }

    // =================== FOLLOW / UNFOLLOW ===================

    suspend fun follow(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        val followingRef = firestore.collection("users")
            .document(currentUserId).collection("following").document(targetUserId)
        batch.set(followingRef, mapOf("followedAt" to now))

        val followerRef = firestore.collection("users")
            .document(targetUserId).collection("followers").document(currentUserId)
        batch.set(followerRef, mapOf("followedAt" to now))

        batch.update(
            firestore.collection("users").document(currentUserId),
            "followingCount", FieldValue.increment(1)
        )
        batch.update(
            firestore.collection("users").document(targetUserId),
            "followersCount", FieldValue.increment(1)
        )
        batch.commit().await()
    }

    suspend fun unfollow(currentUserId: String, targetUserId: String) {
        val batch = firestore.batch()
        val followingRef = firestore.collection("users")
            .document(currentUserId).collection("following").document(targetUserId)
        batch.delete(followingRef)
        val followerRef = firestore.collection("users")
            .document(targetUserId).collection("followers").document(currentUserId)
        batch.delete(followerRef)
        batch.update(
            firestore.collection("users").document(currentUserId),
            "followingCount", FieldValue.increment(-1)
        )
        batch.update(
            firestore.collection("users").document(targetUserId),
            "followersCount", FieldValue.increment(-1)
        )
        batch.commit().await()
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(currentUserId).collection("following").document(targetUserId)
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    /**
     * Lista de usuarios a los que sigue [userId].
     * Filtra los que estén involucrados en un bloqueo (en cualquier dirección).
     */
    suspend fun getFollowing(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId).collection("following")
                .get().await().documents.map { it.id }
            if (ids.isEmpty()) return emptyList()
            val hidden = getMutuallyHiddenIds(userId)
            fetchUsersByIds(ids.filter { it !in hidden })
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Lista de seguidores de [userId].
     * Filtra los bloqueos en cualquier dirección.
     */
    suspend fun getFollowers(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId).collection("followers")
                .get().await().documents.map { it.id }
            if (ids.isEmpty()) return emptyList()
            val hidden = getMutuallyHiddenIds(userId)
            fetchUsersByIds(ids.filter { it !in hidden })
        } catch (e: Exception) { emptyList() }
    }

    /**
     * IDs de usuarios seguidos. Filtra bloqueos para que la sección
     * "De tus amigos" en Home y los listados de reseñas de amigos
     * no incluyan a nadie con bloqueo activo.
     */
    suspend fun getFollowingIds(userId: String): List<String> {
        return try {
            val ids = firestore.collection("users")
                .document(userId).collection("following")
                .get().await().documents.map { it.id }
            if (ids.isEmpty()) return emptyList()
            val hidden = getMutuallyHiddenIds(userId)
            ids.filter { it !in hidden }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchUsersByIds(ids: List<String>): List<PublicUser> {
        if (ids.isEmpty()) return emptyList()
        val chunks = ids.chunked(30)
        val result = mutableListOf<PublicUser>()
        for (chunk in chunks) {
            val docs = firestore.collection("users")
                .whereIn("userId", chunk)
                .get().await()
            result += docs.toObjects(PublicUser::class.java)
        }
        return result
    }

    // =================== BLOCKING ===================
    //
    // Modelo:
    //   users/{userId}/blocks/{blockedUserId}  -> { blockedUserId, createdAt }
    //
    // Comprobar "A bloqueó a B" = un get directo (cheap, sin índice).
    // Para listas grandes (search, followers, following) usamos:
    //   - getBlockedByMeIds(me)   : leer subcolección "blocks" de mi propio doc.
    //   - getUsersWhoBlockedMeIds(me) : collectionGroup query sobre "blocks".
    //
    // La segunda requiere un índice en collectionGroup="blocks", campo "blockedUserId".

    private fun blockRef(currentUserId: String, blockedUserId: String) =
        firestore.collection("users")
            .document(currentUserId)
            .collection("blocks")
            .document(blockedUserId)

    /**
     * Bloquea a [targetUserId] desde [currentUserId].
     *
     * En la misma operación batch:
     *  1) Crea el doc en users/{currentUserId}/blocks/{targetUserId}.
     *  2) Rompe los follows en ambas direcciones (si existían).
     *  3) Decrementa los contadores de followers/following correspondientes.
     *
     * No borra reviews, likes ni comentarios: simplemente quedan filtrados al leer.
     * Así, al desbloquear, todo vuelve a aparecer sin pérdida.
     */
    suspend fun blockUser(currentUserId: String, targetUserId: String) {
        if (currentUserId.isBlank() || targetUserId.isBlank()) return
        if (currentUserId == targetUserId) return

        val batch = firestore.batch()

        // 1) Crear el doc de bloqueo
        batch.set(
            blockRef(currentUserId, targetUserId),
            mapOf(
                "blockedUserId" to targetUserId,
                "createdAt" to System.currentTimeMillis()
            )
        )

        // 2 + 3) Romper follows en ambas direcciones si existen
        val iFollowThem = firestore.collection("users")
            .document(currentUserId).collection("following").document(targetUserId)
        val iAmFollower = firestore.collection("users")
            .document(targetUserId).collection("followers").document(currentUserId)

        val theyFollowMe = firestore.collection("users")
            .document(targetUserId).collection("following").document(currentUserId)
        val theyAreFollower = firestore.collection("users")
            .document(currentUserId).collection("followers").document(targetUserId)

        // Comprobamos cada dirección antes de borrar para no decrementar contadores
        // que no debían moverse.
        val iFollowThemExists = iFollowThem.get().await().exists()
        val theyFollowMeExists = theyFollowMe.get().await().exists()

        if (iFollowThemExists) {
            batch.delete(iFollowThem)
            batch.delete(iAmFollower)
            batch.update(
                firestore.collection("users").document(currentUserId),
                "followingCount", FieldValue.increment(-1)
            )
            batch.update(
                firestore.collection("users").document(targetUserId),
                "followersCount", FieldValue.increment(-1)
            )
        }
        if (theyFollowMeExists) {
            batch.delete(theyFollowMe)
            batch.delete(theyAreFollower)
            batch.update(
                firestore.collection("users").document(targetUserId),
                "followingCount", FieldValue.increment(-1)
            )
            batch.update(
                firestore.collection("users").document(currentUserId),
                "followersCount", FieldValue.increment(-1)
            )
        }

        batch.commit().await()
    }

    /** Desbloquea borrando el doc. Los follows previos NO se restauran. */
    suspend fun unblockUser(currentUserId: String, targetUserId: String) {
        if (currentUserId.isBlank() || targetUserId.isBlank()) return
        blockRef(currentUserId, targetUserId).delete().await()
    }

    /** True si [currentUserId] tiene bloqueado a [otherUserId]. */
    suspend fun didIBlock(currentUserId: String, otherUserId: String): Boolean {
        if (currentUserId.isBlank() || otherUserId.isBlank()) return false
        return try {
            blockRef(currentUserId, otherUserId).get().await().exists()
        } catch (_: Exception) { false }
    }

    /** True si [otherUserId] tiene bloqueado a [currentUserId]. */
    suspend fun didTheyBlockMe(currentUserId: String, otherUserId: String): Boolean {
        if (currentUserId.isBlank() || otherUserId.isBlank()) return false
        return try {
            blockRef(otherUserId, currentUserId).get().await().exists()
        } catch (_: Exception) { false }
    }

    /**
     * Devuelve la relación de bloqueo desde el punto de vista de [currentUserId].
     * Si ambos se bloquearon mutuamente, devuelve IBlockedThem (ver doc de
     * BlockRelation para la justificación).
     */
    suspend fun getBlockRelation(currentUserId: String, otherUserId: String): BlockRelation {
        if (currentUserId.isBlank() || otherUserId.isBlank() || currentUserId == otherUserId) {
            return BlockRelation.NotBlocked
        }
        val iBlocked = didIBlock(currentUserId, otherUserId)
        if (iBlocked) return BlockRelation.IBlockedThem
        val theyBlocked = didTheyBlockMe(currentUserId, otherUserId)
        if (theyBlocked) return BlockRelation.TheyBlockedMe
        return BlockRelation.NotBlocked
    }

    /** IDs de usuarios bloqueados por [userId] (directo: una lectura de subcolección). */
    suspend fun getBlockedByMeIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("blocks")
                .get().await()
                .documents.map { it.id }
                .toSet()
        } catch (_: Exception) { emptySet() }
    }

    /**
     * IDs de usuarios que han bloqueado a [userId].
     * Usa collectionGroup query: requiere índice en collectionGroup "blocks"
     * por campo "blockedUserId". Firestore te dará el enlace en el primer error.
     */
    suspend fun getUsersWhoBlockedMeIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            firestore.collectionGroup("blocks")
                .whereEqualTo("blockedUserId", userId)
                .get().await()
                .documents.mapNotNull { doc ->
                    // El parent de cada doc es la subcolección "blocks";
                    // su parent.parent es el doc del usuario que bloqueó.
                    doc.reference.parent.parent?.id
                }
                .toSet()
        } catch (_: Exception) { emptySet() }
    }

    /**
     * Conjunto unificado de IDs que deben quedar OCULTOS para [userId]
     * en cualquier listado social: los que él bloqueó + los que le bloquearon.
     */
    suspend fun getMutuallyHiddenIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        val blockedByMe = getBlockedByMeIds(userId)
        val blockedMe = getUsersWhoBlockedMeIds(userId)
        return blockedByMe + blockedMe
    }

    /**
     * Devuelve los datos completos (PublicUser) de los usuarios bloqueados por [userId].
     * Combina:
     *   1) IDs desde la subcolección users/{userId}/blocks
     *   2) Lectura batched de los docs de users con whereIn (en lotes de 30).
     *
     * Solo se usa en la pantalla "Usuarios bloqueados" del perfil propio.
     */
    suspend fun getBlockedUsers(userId: String): List<PublicUser> {
        if (userId.isBlank()) return emptyList()
        val ids = getBlockedByMeIds(userId).toList()
        if (ids.isEmpty()) return emptyList()
        return fetchUsersByIds(ids)
    }

    // =================== RESEÑAS ===================

    private fun reviewRef(authorUserId: String, mediaType: String, mediaId: Int) =
        firestore.collection("users")
            .document(authorUserId)
            .collection("reviews")
            .document("${mediaType}_$mediaId")

    suspend fun upsertReview(
        userId: String,
        userName: String,
        userPicture: String?,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        releaseYear: String,
        rating: Float,
        comment: String,
        isFavorite: Boolean
    ) {
        val ref = reviewRef(userId, mediaType, mediaId)
        // Preservamos contadores existentes al actualizar
        val existing = ref.get().await()
        val likesCount = existing.getLong("likesCount") ?: 0L
        val commentsCount = existing.getLong("commentsCount") ?: 0L

        ref.set(
            mapOf(
                "userId" to userId,
                "userName" to userName,
                "userPicture" to userPicture,
                "mediaType" to mediaType,
                "mediaId" to mediaId,
                "title" to title,
                "posterPath" to posterPath,
                "releaseYear" to releaseYear,
                "rating" to rating,
                "comment" to comment,
                "hasComment" to comment.isNotBlank(),
                "isFavorite" to isFavorite,
                "likesCount" to likesCount,
                "commentsCount" to commentsCount,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()

        // Auto-marca como visto en la library al reseñar
        setLibraryStatus(
            userId = userId,
            mediaType = mediaType,
            mediaId = mediaId,
            status = "watched",
            isFavorite = isFavorite,
            title = title,
            posterPath = posterPath,
            releaseYear = releaseYear,
            hasReview = true
        )
    }

    suspend fun deleteReview(userId: String, mediaType: String, mediaId: Int) {
        reviewRef(userId, mediaType, mediaId).delete().await()
        // Marcamos library.hasReview = false (sigue como visto)
        val libRef = libraryRef(userId, mediaType, mediaId)
        if (libRef.get().await().exists()) {
            libRef.update("hasReview", false).await()
        }
    }

    /**
     * Reseñas recientes de los usuarios seguidos para alimentar la sección
     * "De tus amigos" en Home. NOTA: el filtrado de bloqueos ya ocurre en
     * getFollowingIds (que es lo que el repository llama antes de pasar
     * los IDs aquí), así que esta función no necesita filtrar de nuevo.
     */
    suspend fun getFriendsReviewedItems(
        followingIds: List<String>,
        mediaType: String,
        limitPerFriend: Int = 10
    ): List<FriendActivity> {
        if (followingIds.isEmpty()) return emptyList()
        val users = fetchUsersByIds(followingIds).associateBy { it.userId }

        val result = mutableListOf<FriendActivity>()
        for (friendId in followingIds) {
            try {
                val docs = firestore.collection("users")
                    .document(friendId)
                    .collection("reviews")
                    .whereEqualTo("mediaType", mediaType)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limitPerFriend.toLong())
                    .get().await()

                val friend = users[friendId] ?: continue
                docs.documents.forEach { d ->
                    val mediaId = (d.getLong("mediaId") ?: 0L).toInt()
                    val rating = d.getDouble("rating")?.toFloat()
                    val hasComment = d.getBoolean("hasComment")
                        ?: !d.getString("comment").isNullOrBlank()
                    val isFavorite = d.getBoolean("isFavorite") ?: false

                    result += FriendActivity(
                        mediaId = mediaId,
                        mediaType = mediaType,
                        title = d.getString("title") ?: "",
                        posterPath = d.getString("posterPath"),
                        releaseYear = d.getString("releaseYear") ?: "",
                        watchedAt = d.getLong("createdAt") ?: 0L,
                        friendUserId = friend.userId,
                        friendName = friend.name,
                        friendPicture = friend.pictureUrl,
                        reviewId = FriendReview.composeId(friend.userId, mediaType, mediaId),
                        rating = rating,
                        hasComment = hasComment,
                        isFavorite = isFavorite
                    )
                }
            } catch (_: Exception) { }
        }
        return result.sortedByDescending { it.watchedAt }
    }

    /**
     * Devuelve TODAS las reseñas de un usuario concreto.
     * Lectura directa de la subcolección users/{userId}/reviews — no es collectionGroup,
     * así que no requiere índice especial.
     *
     * Se usa para:
     *  - Calcular los counts en el perfil compactado.
     *  - Construir las secciones de pelis/series/juegos en UserReviewsScreen.
     *  - Saber qué items de "visto" tienen reseña asociada (para badge de nota).
     *
     * No filtra bloqueos: el caller decide. En la práctica solo se llama desde
     * UserProfileViewModel cuando ya se ha comprobado que NO hay bloqueo activo.
     */
    suspend fun getReviewsByUser(authorUserId: String): List<FriendReview> {
        if (authorUserId.isBlank()) return emptyList()
        return try {
            val docs = firestore.collection("users")
                .document(authorUserId)
                .collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            docs.documents.mapNotNull { d ->
                val mediaType = d.getString("mediaType") ?: return@mapNotNull null
                val mediaId = (d.getLong("mediaId") ?: return@mapNotNull null).toInt()
                // No nos interesa likedByMe aquí (es una vista de perfil ajeno),
                // así que pasamos false para evitar lecturas extra por cada review.
                mapReviewDoc(d, authorUserId, mediaType, mediaId, likedByMe = false)
            }
        } catch (_: Exception) { emptyList() }
    }

    // =================== INTERACCIÓN: LIKES / COMMENTS ===================

    suspend fun getReviewById(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): FriendReview? {
        return try {
            val snap = reviewRef(authorUserId, mediaType, mediaId).get().await()
            if (!snap.exists()) return null
            val likedByMe = if (currentUserId.isBlank()) false
            else reviewRef(authorUserId, mediaType, mediaId)
                .collection("likes").document(currentUserId).get().await().exists()
            mapReviewDoc(snap, authorUserId, mediaType, mediaId, likedByMe)
        } catch (e: Exception) { null }
    }

    suspend fun getCommentsForReview(
        authorUserId: String,
        mediaType: String,
        mediaId: Int
    ): List<ReviewComment> {
        return try {
            reviewRef(authorUserId, mediaType, mediaId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
                .documents.mapNotNull { d ->
                    d.toObject(ReviewComment::class.java)?.copy(commentId = d.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun toggleLike(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): Boolean {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        val likeRef = ref.collection("likes").document(currentUserId)
        val exists = likeRef.get().await().exists()
        return if (exists) {
            likeRef.delete().await()
            ref.update("likesCount", FieldValue.increment(-1)).await()
            false
        } else {
            likeRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
            ref.update("likesCount", FieldValue.increment(1)).await()
            true
        }
    }

    suspend fun addComment(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        commenterUserId: String,
        commenterName: String,
        commenterPicture: String?,
        text: String
    ) {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        val commentRef = ref.collection("comments").document()
        commentRef.set(
            mapOf(
                "commentId" to commentRef.id,
                "userId" to commenterUserId,
                "userName" to commenterName,
                "userPicture" to commenterPicture,
                "text" to text,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        ref.update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        commentId: String
    ) {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        ref.collection("comments").document(commentId).delete().await()
        ref.update("commentsCount", FieldValue.increment(-1)).await()
    }

    /**
     * Reseñas de TODOS los usuarios para un media, ordenadas por likes desc.
     * Filtra reseñas de usuarios con bloqueo activo en cualquier dirección.
     * Requiere índice (collectionGroup="reviews": mediaType, mediaId, likesCount, createdAt).
     */
    suspend fun getAllReviewsForMedia(
        mediaType: String,
        mediaId: Int,
        currentUserId: String,
        limit: Long = 50
    ): List<FriendReview> {
        return try {
            val docs = firestore.collectionGroup("reviews")
                .whereEqualTo("mediaType", mediaType)
                .whereEqualTo("mediaId", mediaId)
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()

            // Calculamos el set de bloqueados (yo a ellos + ellos a mí) una sola vez.
            // Pedimos algo de margen porque después filtramos, así no nos quedamos
            // cortos si la mayoría de top results son de bloqueados.
            val hidden = if (currentUserId.isBlank()) emptySet()
            else getMutuallyHiddenIds(currentUserId)

            docs.documents.mapNotNull { d ->
                val authorUserId = d.getString("userId") ?: return@mapNotNull null
                if (authorUserId in hidden) return@mapNotNull null
                val likedByMe = if (currentUserId.isBlank()) false
                else d.reference.collection("likes")
                    .document(currentUserId).get().await().exists()
                mapReviewDoc(d, authorUserId, mediaType, mediaId, likedByMe)
            }
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Reseñas SOLO de los amigos sobre un media, ordenadas por likes desc.
     * Los IDs ya vienen filtrados por bloqueo desde getFollowingIds().
     */
    suspend fun getFriendsReviewsForMedia(
        followingIds: List<String>,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): List<FriendReview> {
        if (followingIds.isEmpty()) return emptyList()
        val result = mutableListOf<FriendReview>()
        for (friendId in followingIds) {
            getReviewById(friendId, mediaType, mediaId, currentUserId)?.let { result += it }
        }
        return result.sortedByDescending { it.likesCount }
    }

    private fun mapReviewDoc(
        d: DocumentSnapshot,
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        likedByMe: Boolean
    ): FriendReview {
        return FriendReview(
            reviewId = FriendReview.composeId(authorUserId, mediaType, mediaId),
            userId = authorUserId,
            userName = d.getString("userName") ?: "",
            userPicture = d.getString("userPicture"),
            mediaId = mediaId,
            mediaType = mediaType,
            mediaTitle = d.getString("title") ?: "",
            mediaPosterPath = d.getString("posterPath"),
            releaseYear = d.getString("releaseYear") ?: "",
            rating = d.getDouble("rating")?.toFloat(),
            comment = d.getString("comment") ?: "",
            isFavorite = d.getBoolean("isFavorite") ?: false,
            createdAt = d.getLong("createdAt") ?: 0L,
            likesCount = (d.getLong("likesCount") ?: 0L).toInt(),
            commentsCount = (d.getLong("commentsCount") ?: 0L).toInt(),
            likedByMe = likedByMe
            // entryKind se queda con default REVIEW (los doc en /reviews son reseñas reales)
        )
    }

    // =================== TOP 3 ===================

    suspend fun setTopItem(
        userId: String,
        mediaType: String,
        position: Int,
        mediaId: Int,
        title: String,
        posterPath: String?
    ) {
        val docId = "${mediaType}_$position"
        firestore.collection("users")
            .document(userId)
            .collection("top_items")
            .document(docId)
            .set(
                mapOf(
                    "mediaType" to mediaType,
                    "position" to position,
                    "mediaId" to mediaId,
                    "title" to title,
                    "posterPath" to posterPath
                )
            ).await()
    }

    suspend fun removeTopItem(userId: String, mediaType: String, position: Int) {
        val docId = "${mediaType}_$position"
        firestore.collection("users")
            .document(userId)
            .collection("top_items")
            .document(docId)
            .delete().await()
    }

    suspend fun getTopItems(userId: String): List<Map<String, Any?>> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("top_items")
                .get().await()
                .documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =================== LIBRARY (watchlist + watched + favoritos) ===================

    private fun libraryRef(userId: String, mediaType: String, mediaId: Int) =
        firestore.collection("users")
            .document(userId)
            .collection("library")
            .document("${mediaType}_$mediaId")

    /**
     * Crea/actualiza una entrada de library con el status indicado.
     * Si ya tenía una entrada con status "watched", NO la rebaja a "watchlist".
     * Solo actualiza los campos que se pasan; el resto se hace merge.
     */
    suspend fun setLibraryStatus(
        userId: String,
        mediaType: String,
        mediaId: Int,
        status: String,                 // "watched" | "watchlist"
        isFavorite: Boolean? = null,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null,
        hasReview: Boolean? = null
    ) {
        val ref = libraryRef(userId, mediaType, mediaId)
        val existing = ref.get().await()

        // No rebajar de watched a watchlist
        val finalStatus = if (existing.getString("status") == "watched" && status == "watchlist")
            "watched" else status

        val data = mutableMapOf<String, Any?>(
            "mediaType" to mediaType,
            "mediaId" to mediaId,
            "status" to finalStatus,
            "updatedAt" to System.currentTimeMillis()
        )
        if (isFavorite != null) data["isFavorite"] = isFavorite
        if (title != null) data["title"] = title
        if (posterPath != null) data["posterPath"] = posterPath
        if (releaseYear != null) data["releaseYear"] = releaseYear
        if (hasReview != null) data["hasReview"] = hasReview

        ref.set(data, SetOptions.merge()).await()
    }

    /** Cambia solo el flag de favorito sin tocar status. Crea la entrada si no existía. */
    suspend fun setLibraryFavorite(
        userId: String,
        mediaType: String,
        mediaId: Int,
        isFavorite: Boolean,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null
    ) {
        val ref = libraryRef(userId, mediaType, mediaId)
        val existing = ref.get().await()
        val data = mutableMapOf<String, Any?>(
            "mediaType" to mediaType,
            "mediaId" to mediaId,
            "isFavorite" to isFavorite,
            "updatedAt" to System.currentTimeMillis()
        )
        if (!existing.exists()) {
            data["status"] = "watchlist"
        }
        if (title != null) data["title"] = title
        if (posterPath != null) data["posterPath"] = posterPath
        if (releaseYear != null) data["releaseYear"] = releaseYear

        ref.set(data, SetOptions.merge()).await()
    }

    /** Borra la entrada de library completamente. */
    suspend fun removeLibraryEntry(userId: String, mediaType: String, mediaId: Int) {
        libraryRef(userId, mediaType, mediaId).delete().await()
    }

    /** Devuelve la entrada de library (o null si no existe). */
    suspend fun getLibraryEntry(userId: String, mediaType: String, mediaId: Int): LibraryEntry? {
        return try {
            val snap = libraryRef(userId, mediaType, mediaId).get().await()
            if (!snap.exists()) null else snap.toObject(LibraryEntry::class.java)
        } catch (_: Exception) { null }
    }

    /**
     * Devuelve TODAS las entradas de library de un usuario, ordenadas por updatedAt desc.
     * Filtra en el cliente por status / mediaType / favorito.
     */
    suspend fun getLibrary(userId: String): List<LibraryEntry> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("library")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(LibraryEntry::class.java)
        } catch (_: Exception) { emptyList() }
    }
}