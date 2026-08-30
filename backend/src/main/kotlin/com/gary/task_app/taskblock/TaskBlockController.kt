package com.gary.task_app.taskblock

import com.gary.task_app.taskblock.dto.BlockResponseDto
import com.gary.task_app.taskblock.dto.CreateBlockRequestDto
import com.gary.task_app.taskblock.dto.ReorderBlocksRequestDto
import com.gary.task_app.taskblock.dto.UpdateBlockRequestDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/blocks")
class TaskBlockController(
    private val taskBlockService: TaskBlockService,
) {

    @PostMapping
    fun create(
        @Valid @RequestBody req: CreateBlockRequestDto,
        authentication: Authentication
    ): ResponseEntity<BlockResponseDto> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(taskBlockService.createBlock(req, authentication.name))

    @PatchMapping(path = ["/{id}"])
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: UpdateBlockRequestDto,
        authentication: Authentication
    ): ResponseEntity<BlockResponseDto> =
        ResponseEntity.status(HttpStatus.OK)
            .body(taskBlockService.updateBlock(req, authentication.name,id))

    @PatchMapping("/reorder")
    fun reorder(
        @Valid @RequestBody req: ReorderBlocksRequestDto,
        authentication: Authentication
    ): ResponseEntity<List<BlockResponseDto>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(taskBlockService.reorderBlocks(req, authentication.name))


    @DeleteMapping(path = ["/{id}"])
    fun delete(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<Void> {
        taskBlockService.deleteBlock(authentication.name, id)
        return ResponseEntity.noContent().build()
    }


    @GetMapping
    fun list(
        @RequestParam date: LocalDate,
        authentication: Authentication
    ): List<BlockResponseDto> =
        taskBlockService.listBlocks(authentication.name, date)
}
