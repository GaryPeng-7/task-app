package com.gary.task_app.timer.dto

import com.gary.task_app.timer.Timer
import com.gary.task_app.timer.TimerState
import java.time.Instant
import java.util.UUID

data class TimerStateResponse(
    val state: TimerState,
    val endAt: Instant?,
    val remainingSeconds: Int?,
    val workSeconds: Int,
    val breakSeconds: Int,
    val blockId: UUID?
) {
    companion object {
        fun from(timer: Timer) = TimerStateResponse(
            state = timer.timerState,
            endAt = timer.endsAt,
            remainingSeconds = timer.remainingSeconds,
            workSeconds = timer.workSeconds,
            breakSeconds = timer.breakSeconds,
            blockId = timer.blockId
        )
    }
}
