package com.gary.task_app.auth

import com.gary.task_app.auth.dto.LoginRequestDto
import com.gary.task_app.auth.dto.RegisterRequestDto
import com.gary.task_app.auth.dto.UserDto
import com.gary.task_app.config.PasswordConfig
import com.gary.task_app.user.User
import com.gary.task_app.user.UserRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.AbstractPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/auth"])
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    @PostMapping("/register")
    fun registerUser(
        @Valid @RequestBody req: RegisterRequestDto
    ): ResponseEntity<Any> {
        if (userRepository.existsByEmail(req.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        val user = userRepository.save(
            User(
                email = req.email,
                passwordHash = requireNotNull(passwordEncoder.encode(req.password))
                { "BCrypt encode returned null" }
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(user.toDto())
    }

    @PostMapping("/login")
    fun loginUser(
        @Valid @RequestBody req: LoginRequestDto
    ): ResponseEntity<Any> {
        val user = userRepository.findByEmail(req.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!passwordEncoder.matches(req.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok(mapOf("token" to jwtService.generateToken(user)))
    }
}
