package com.gary.task_app.message

import com.gary.task_app.auth.JwtService
import io.jsonwebtoken.JwtException
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class WebSocketAuthInterceptor(private val jwtService: JwtService): ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)!!
        if (accessor.command != StompCommand.CONNECT) return message

        // extract
        val header = accessor.getFirstNativeHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            throw IllegalArgumentException("Missing token")
        }
        try {
            // validate
            val email = jwtService.extractEmail(header.removePrefix("Bearer "))
            accessor.user = UsernamePasswordAuthenticationToken(email, null, emptyList())
        } catch (ex: JwtException) {
            throw IllegalArgumentException("Invalid or expired token")
        }
        return message
    }
}
