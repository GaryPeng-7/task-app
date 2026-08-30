package com.gary.task_app.taskblock.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateBlockRequestDto(
    val subject: String,
    val description: String?,
    @field:NotNull val blockDate: LocalDate
)
