package com.esha.fitsync.models

data class AiChatMessage(
    val role: String = "assistant", // "user" or "assistant"
    val content: String = "",
    val timestamp: String? = null
)

data class AskAiRequest(
    val message: String,
    val conversationId: String? = null
)

data class AskAiResponse(
    val reply: String = "",
    val conversationId: String? = null,
    val disclaimer: String? = null
)

data class AiHistoryResponse(
    val conversationId: String? = null,
    val messages: List<AiChatMessage> = emptyList()
)
