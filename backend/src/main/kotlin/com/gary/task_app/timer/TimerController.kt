package com.gary.task_app.timer

import com.gary.task_app.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/blocks/{blockId}/timer")
class TimerController(
    private val timerService: TimerService,
    private val userRepository: UserRepository
) {

    @PostMapping("/start")
    fun start(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.start(blockId, currentUserId(auth))

    @PostMapping("/pause")
    fun pause(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.pause(blockId, currentUserId(auth))

    @PostMapping("/resume")
    fun resume(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.resume(blockId, currentUserId(auth))

    @PostMapping("/complete")
    fun complete(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.complete(blockId, currentUserId(auth))

    @PostMapping("/skip")
    fun skip(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.skipBreak(blockId, currentUserId(auth))

    @GetMapping
    fun getState(@PathVariable blockId: UUID, auth: Authentication) =
        timerService.getState(blockId, currentUserId(auth))

    private fun currentUserId(auth: Authentication): UUID =
        userRepository.findByEmail(auth.name)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
}
