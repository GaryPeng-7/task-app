package com.gary.task_app.taskblock

import com.gary.task_app.taskblock.dto.BlockResponseDto
import com.gary.task_app.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class BlockStatus { TO_BE_DONE, IN_PROGRESS, FINISHED}

@Entity
@Table(name = "task_blocks")
class TaskBlock(

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    var subject: String? = null,
    var description: String? = null,

    @Column(name = "block_date", nullable = false)
    var blockDate: LocalDate,

    @Column(nullable = false)
    var position: Int,

    //@Column(name = "remaining_seconds")
    //var remainingSeconds: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BlockStatus = BlockStatus.TO_BE_DONE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
    ) {
    fun toDto() = BlockResponseDto(id = id, subject = subject, description = description,
        blockDate = blockDate, position = position,
        //remainingSeconds = remainingSeconds,
        status = status)
}
