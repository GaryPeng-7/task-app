package com.gary.task_app.taskblock.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class ReorderBlocksRequestDto(
    @field:NotNull val blockDate: LocalDate,
    @field:NotEmpty val orderIds: List<UUID>
)
