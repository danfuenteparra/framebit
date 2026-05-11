package com.example.moviebox.data.repository

import com.example.moviebox.data.remote.firebase.FirestoreService
import com.example.moviebox.data.remote.model.ChatMessage
import com.example.moviebox.data.remote.model.ChatThread
import com.example.moviebox.data.remote.model.MessageAttachment
import com.example.moviebox.data.remote.model.PublicUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val firestore: FirestoreService
) {

    suspend fun ensureSignedIn() = firestore.ensureFirebaseSignedIn()

    /**
     * Comprueba si dos usuarios se siguen mutuamente. Devuelve true si pueden chatear.
     */
    suspend fun canChatWith(currentUserId: String, otherUserId: String): Boolean {
        if (currentUserId.isBlank() || otherUserId.isBlank() || currentUserId == otherUserId) return false
        val iFollowOther = firestore.isFollowing(currentUserId, otherUserId)
        val otherFollowsMe = firestore.isFollowing(otherUserId, currentUserId)
        return iFollowOther && otherFollowsMe
    }

    suspend fun startChat(currentUserId: String, otherUserId: String): String =
        firestore.ensureChatThread(currentUserId, otherUserId)

    suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) = firestore.sendMessage(chatId, senderId, receiverId, text, type = "text")

    suspend fun sendMediaMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        releaseYear: String,
        captionText: String = ""
    ) {
        val attachment = MessageAttachment(
            mediaType = mediaType,
            mediaId = mediaId,
            title = title,
            posterPath = posterPath,
            releaseYear = releaseYear
        )
        firestore.sendMessage(chatId, senderId, receiverId, captionText, type = "media", attachment = attachment)
    }

    suspend fun sendReviewMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        reviewId: String,
        mediaType: String,
        mediaId: Int,
        mediaTitle: String,
        mediaPosterPath: String?,
        releaseYear: String,
        rating: Float?,
        authorName: String,
        authorPicture: String?,
        captionText: String = ""
    ) {
        val attachment = MessageAttachment(
            mediaType = mediaType,
            mediaId = mediaId,
            title = mediaTitle,
            posterPath = mediaPosterPath,
            releaseYear = releaseYear,
            reviewId = reviewId,
            reviewRating = rating,
            reviewAuthorName = authorName,
            reviewAuthorPicture = authorPicture
        )
        firestore.sendMessage(chatId, senderId, receiverId, captionText, type = "review", attachment = attachment)
    }

    suspend fun markChatAsRead(chatId: String, userId: String) =
        firestore.markChatAsRead(chatId, userId)

    fun observeChats(userId: String): Flow<List<ChatThread>> =
        firestore.observeChats(userId)

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        firestore.observeMessages(chatId)

    fun observeTotalUnread(userId: String): Flow<Int> =
        firestore.observeTotalUnread(userId)

    /** Lista de usuarios con seguimiento mutuo, para iniciar chat. */
    suspend fun getMutualFollows(userId: String): List<PublicUser> =
        firestore.getMutualFollows(userId)
}