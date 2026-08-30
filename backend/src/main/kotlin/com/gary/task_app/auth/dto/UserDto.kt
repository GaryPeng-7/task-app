package com.gary.task_app.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class UserDto(
    @field:NotBlank val id: UUID,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val createdAt: Instant
)
