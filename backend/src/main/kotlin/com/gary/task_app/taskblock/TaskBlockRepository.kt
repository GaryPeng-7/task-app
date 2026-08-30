package com.gary.task_app.taskblock

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface TaskBlockRepository: JpaRepository<TaskBlock, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): TaskBlock?
    fun findAllByUserIdAndBlockDateOrderByPositionAsc(
        userId: UUID, blockDate: LocalDate): List<TaskBlock>
}
