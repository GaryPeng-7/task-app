package com.gary.task_app.taskblock.dto

import com.gary.task_app.taskblock.BlockStatus

data class UpdateBlockRequestDto(
    val subject: String? = null,
    val description: String? = null,
    val status: BlockStatus? = null,
    val remainingSeconds: Int? = null
)
