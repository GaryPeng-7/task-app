package com.gary.task_app.message.dto

import java.time.Instant

data class ChatMessageOut(
    val sender: String,
    val content:String,
    val timestamp: Instant
)
