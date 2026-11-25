package com.gary.task_app.timer.dto

import com.gary.task_app.timer.Timer
import com.gary.task_app.timer.TimerState
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.util.UUID
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

data class TimerStateResponse(
    val state: TimerState,
    val endAt: Instant?,
    val remainingSeconds: Int?,
    val serverRemainingSeconds: Int,
    val workSeconds: Int,
    val breakSeconds: Int,
    val blockId: UUID?
) {
    companion object {
        fun from(timer: Timer, now: Instant) = TimerStateResponse(
            state = timer.timerState,
            endAt = timer.endsAt,
            remainingSeconds = timer.remainingSeconds,
            serverRemainingSeconds = when (timer.timerState) {
                TimerState.WORK_RUNNING, TimerState.BREAK_RUNNING ->
                    timer.endsAt?.let {
                        max(0, Duration.between(now, it).seconds.toInt())
                    } ?: 0
                TimerState.WORK_PAUSED, TimerState.BREAK_PAUSED ->
                    timer.remainingSeconds ?: 0
                else -> timer.workSeconds
            },
            workSeconds = timer.workSeconds,
            breakSeconds = timer.breakSeconds,
            blockId = timer.blockId
        )
    }
}
