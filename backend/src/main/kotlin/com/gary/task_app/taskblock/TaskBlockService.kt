package com.gary.task_app.taskblock

import com.gary.task_app.taskblock.dto.BlockResponseDto
import com.gary.task_app.taskblock.dto.CreateBlockRequestDto
import com.gary.task_app.taskblock.dto.ReorderBlocksRequestDto
import com.gary.task_app.taskblock.dto.UpdateBlockRequestDto
import com.gary.task_app.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Service
class TaskBlockService(
    private val taskBlockRepository: TaskBlockRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createBlock(request: CreateBlockRequestDto, email: String):
    BlockResponseDto{
        val user = currentUser(email)
        val nextPosition = (taskBlockRepository.
        findAllByUserIdAndBlockDateOrderByPositionAsc(user.id, request.blockDate)
            .lastOrNull()?.position ?: 0) + 1

        val newBlock = TaskBlock(
            subject = request.subject,
            description = request.description,
            blockDate = request.blockDate,
            position = nextPosition,
            user = user)

        taskBlockRepository.save(newBlock)
        return newBlock.toDto()
    }

    @Transactional
    fun updateBlock(request: UpdateBlockRequestDto, email: String, blockId: UUID):
            BlockResponseDto{
        val user = currentUser(email)

        val block = taskBlockRepository.findByIdAndUserId(blockId, user.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found")

        request.subject?.let {block.subject = it}
        request.description?.let {block.description = it}
        request.status?.let {block.status = it}
        //request.remainingSeconds?.let { block.remainingSeconds = it }

        return taskBlockRepository.save(block).toDto()
    }

    @Transactional
    fun deleteBlock(email: String, blockId: UUID) {
        val user = currentUser(email)

        val block = taskBlockRepository.findByIdAndUserId(blockId, user.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found")

        taskBlockRepository.delete(block)
    }

    @Transactional
    fun reorderBlocks(request: ReorderBlocksRequestDto, email: String)
    : List<BlockResponseDto>{
        val user = currentUser(email)

        val blocks = taskBlockRepository
            .findAllByUserIdAndBlockDateOrderByPositionAsc(user.id, request.blockDate)
        val blockById = blocks.associateBy { it.id }

        if (request.orderIds.size != blocks.size ||
            request.orderIds.toSet() != blockById.keys) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid block list for this date")
        }

        request.orderIds.forEachIndexed { index, id ->
            blockById.getValue(id).position = index + 1
        }

        return request.orderIds.map {
            blockById.getValue(it).toDto()
        }
    }

    @Transactional(readOnly = true)
    fun listBlocks(email: String, date: LocalDate): List<BlockResponseDto> {
        val user = currentUser(email)
        return taskBlockRepository.findAllByUserIdAndBlockDateOrderByPositionAsc(
            user.id, date).map { it.toDto()}
    }

    private fun currentUser(email: String) = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
}
