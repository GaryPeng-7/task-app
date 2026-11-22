package com.gary.task_app.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

class GlobalExceptioinHandler {

    data class ApiError(
        val message: String,
        val status: Int,
        val timestamp: Instant = Instant.now()
    )

    @ExceptionHandler(ResponseStatusException::class)
    fun handleStatus(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.statusCode)
            .body(ApiError(ex.reason ?: "Something went wrong",
                ex.statusCode.value()))
}
