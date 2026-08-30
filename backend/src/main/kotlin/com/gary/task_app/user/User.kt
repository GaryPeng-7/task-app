package com.gary.task_app.user

import com.gary.task_app.auth.dto.UserDto
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "users")
class User(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name= "created_at")
    val createdAt: Instant = Instant.now()
) {
    fun toDto() = UserDto(id = id, email = email, createdAt = createdAt)
}
