package com.gary.task_app.me

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MeController {
    @GetMapping("/me")
    fun me(): Map<String, Any?> = mapOf(
        "email" to SecurityContextHolder.getContext().authentication?.name
    )
}
