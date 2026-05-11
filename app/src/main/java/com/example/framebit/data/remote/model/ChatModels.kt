package com.example.framebit.data.remote.model

/**
 * Hilo de chat 1-a-1 entre dos usuarios. Documento Firestore: chats/{chatId}.
 * chatId = los dos userId ordenados alfabéticamente unidos por "__".
 */
data class ChatThread(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    /** Mapa userId -> {"name": String, "picture": String?} */
    val participantInfo: Map<String, Map<String, Any?>> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val lastMessageType: String = "text",  // "text" | "media" | "review"
    /** Mapa userId -> contador de no leídos para ese usuario. */
    val unreadCount: Map<String, Long> = emptyMap()
) {
    constructor() : this("", emptyList(), emptyMap(), "", 0L, "", "text", emptyMap())

    fun otherUserId(myUserId: String): String =
        participants.firstOrNull { it != myUserId } ?: ""

    fun otherUserName(myUserId: String): String =
        participantInfo[otherUserId(myUserId)]?.get("name") as? String ?: ""

    fun otherUserPicture(myUserId: String): String? =
        participantInfo[otherUserId(myUserId)]?.get("picture") as? String

    fun unreadFor(userId: String): Int = (unreadCount[userId] ?: 0L).toInt()

    companion object {
        fun composeChatId(userIdA: String, userIdB: String): String {
            val (a, b) = if (userIdA < userIdB) userIdA to userIdB else userIdB to userIdA
            return "${a}__${b}"
        }
    }
}

/**
 * Mensaje individual. Documento Firestore: chats/{chatId}/messages/{messageId}.
 */
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",  // "text" | "media" | "review"
    val createdAt: Long = 0L,
    val attachment: MessageAttachment? = null
) {
    constructor() : this("", "", "", "text", 0L, null)
}

/**
 * Adjunto cuando type = "media" o "review".
 * Para "review", reviewId es "{authorUserId}::{mediaType}::{mediaId}".
 */
data class MessageAttachment(
    val mediaType: String = "",
    val mediaId: Int = 0,
    val title: String = "",
    val posterPath: String? = null,
    val releaseYear: String = "",
    val reviewId: String? = null,
    val reviewRating: Float? = null,
    val reviewAuthorName: String? = null,
    val reviewAuthorPicture: String? = null
) {
    constructor() : this("", 0, "", null, "", null, null, null, null)
}