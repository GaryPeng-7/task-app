package com.gary.task_app.message

import com.gary.task_app.message.dto.ChatMessageIn
import com.gary.task_app.message.dto.ChatMessageOut
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.util.HtmlUtils
import java.security.Principal
import java.time.Instant

@Controller
class ChattingController {

    @MessageMapping("/chat")
    @SendTo("/topic/lobby")
    fun chatting(message: ChatMessageIn, principal: Principal): ChatMessageOut {
        return ChatMessageOut(
            sender = principal.name,
            content = message.content,
            timestamp = Instant.now()
        )
    }
}
