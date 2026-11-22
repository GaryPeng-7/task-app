package com.gary.task_app.message

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class WebSocketAuthInterceptorTest {

    private val secret = "test-secret-key-that-is-long-enough-for-hmac-sha256"
    private val jwtService = com.gary.task_app.auth.JwtService(secret, 60_000)
    private val interceptor = WebSocketAuthInterceptor(jwtService)

    private val stubChannel = object : MessageChannel {
        override fun send(message: Message<*>): Boolean = true
        override fun send(message: Message<*>, timeout: Long): Boolean = true
    }

    private fun makeToken(email: String): String =
        Jwts.builder()
            .subject(email)
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()

    private fun connectFrame(authHeader: String?): Pair<Message<ByteArray>, StompHeaderAccessor> {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        if (authHeader != null) accessor.setNativeHeader("Authorization", authHeader)
        accessor.setLeaveMutable(true)
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        return message to accessor
    }

    @Test
    fun `CONNECT with valid token sets the principal`() {
        val (message, accessor) = connectFrame("Bearer ${makeToken("alice@example.com")}")
        interceptor.preSend(message, stubChannel)
        assertThat(accessor.user?.name).isEqualTo("alice@example.com")
    }

    @Test
    fun `CONNECT without token is rejected`() {
        val (message, _) = connectFrame(null)
        assertThatThrownBy { interceptor.preSend(message, stubChannel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `CONNECT with garbage token is rejected`() {
        val (message, _) = connectFrame("Bearer not.a.token")
        assertThatThrownBy { interceptor.preSend(message, stubChannel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `non-CONNECT frames pass through without a token`() {
        val accessor = StompHeaderAccessor.create(StompCommand.SEND)
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        assertThat(interceptor.preSend(message, stubChannel)).isSameAs(message)
    }
}
