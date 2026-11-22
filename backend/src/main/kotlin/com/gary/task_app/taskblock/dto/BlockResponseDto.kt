package com.gary.task_app.taskblock.dto

import com.gary.task_app.taskblock.BlockStatus
import java.time.LocalDate
import java.util.UUID

data class BlockResponseDto(
    val id: UUID,
    val subject: String?,
    val description: String?,
    val blockDate: LocalDate,
    val position: Int,
    //val remainingSeconds: Int?,
    val status: BlockStatus
)
