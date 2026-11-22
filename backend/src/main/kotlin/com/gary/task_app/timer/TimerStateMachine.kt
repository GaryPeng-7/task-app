package com.gary.task_app.timer


import java.time.Duration
import java.time.Instant


enum class TimerState {
    IDLE, WORK_RUNNING, WORK_PAUSED, BREAK_RUNNING, BREAK_PAUSED, DONE
}

data class TimerSnapshot(
    val state: TimerState,
    val endAt: Instant? = null,
    val remainingSeconds: Int? = null,
    val workSeconds: Int = 3000,
    val breakSeconds: Int = 300
)

sealed interface TimerIntent {
    data object Start : TimerIntent
    data object Pause : TimerIntent
    data object Resume: TimerIntent
    data object Complete: TimerIntent
    data object SkipBreak: TimerIntent
    //data object Reset : TimerIntent
    data object CheckExpiry: TimerIntent
}

sealed interface TransitionResult

data class Applied(
    val snapshot: TimerSnapshot,
    val blockHasStarted: Boolean = false,
    val blockFinished: Boolean = false,
    val advanceToNextBlock: Boolean = false
): TransitionResult

data class NoOp(val snapshot: TimerSnapshot): TransitionResult

data class Rejected(val reason: String): TransitionResult

object TimerStateMachine {

    fun transition(current: TimerSnapshot, intent: TimerIntent,
                   now: Instant): TransitionResult =
        when(intent) {
            TimerIntent.Start -> start(current, now)
            TimerIntent.Pause -> pause(current, now)
            TimerIntent.Resume -> resume(current, now)
            TimerIntent.Complete -> complete(current, now)
            TimerIntent.SkipBreak -> skip(current, now)
            //TimerIntent.Reset -> reset(current, now)
            TimerIntent.CheckExpiry -> expire(current, now)
        }

    private fun start(current: TimerSnapshot, now: Instant) : TransitionResult =
        when (current.state) {
            TimerState.IDLE -> Applied(current.copy(
                state = TimerState.WORK_RUNNING,
                endAt = now.plusSeconds(current.workSeconds.toLong()),
            ),
                blockHasStarted = true)
            TimerState.WORK_RUNNING -> NoOp(current)
            else -> Rejected("cannot START from ${current.state}")
        }

    private fun pause(current: TimerSnapshot, now: Instant) : TransitionResult =
        when (current.state) {
            TimerState.WORK_RUNNING, TimerState.BREAK_RUNNING -> {
                val endAt = requireNotNull(current.endAt) {
                    "RUNNING without end_at - invariant broken"
                }
                Applied(current.copy(
                    state = if (current.state == TimerState.WORK_RUNNING) {
                        TimerState.WORK_PAUSED
                    } else {
                        TimerState.BREAK_PAUSED
                    },
                    endAt = null,
                    remainingSeconds = Duration.between(now, endAt).seconds.toInt(),
                ))
            }
            TimerState.WORK_PAUSED, TimerState.BREAK_PAUSED -> NoOp(current)
            else -> Rejected("cannot PAUSE from ${current.state}")
        }

    private fun resume(current: TimerSnapshot, now: Instant) : TransitionResult =
        when (current.state) {
            TimerState.WORK_PAUSED, TimerState.BREAK_PAUSED -> {
                val remaining = requireNotNull(current.remainingSeconds) {
                    "PAUSED without remaining_seconds - invariant broken"
                }
                Applied(current.copy(
                    state = if (current.state == TimerState.WORK_PAUSED) {
                        TimerState.WORK_RUNNING
                    } else {
                        TimerState.BREAK_RUNNING
                    },
                    endAt = now.plusSeconds(remaining.toLong()),
                    remainingSeconds = null
                ))
            }
            TimerState.WORK_RUNNING, TimerState.BREAK_RUNNING -> NoOp(current)
            else -> Rejected("cannot RESUME from ${current.state}")
        }

    private fun complete(current: TimerSnapshot, now: Instant) : TransitionResult =
        when (current.state) {
            TimerState.WORK_RUNNING, TimerState.WORK_PAUSED -> {
                Applied(current.copy(
                    state = TimerState.BREAK_RUNNING,
                    endAt = now.plusSeconds(current.breakSeconds.toLong()),
                    remainingSeconds = null
                ),
                    blockFinished = true)
            }
            else -> Rejected("cannot COMPLETE from ${current.state}")
        }

    private fun skip(current: TimerSnapshot, now: Instant): TransitionResult =
        when (current.state) {
            TimerState.BREAK_RUNNING, TimerState.BREAK_PAUSED -> {
                Applied(current.copy(
                    state = TimerState.DONE,
                    endAt = null,
                    remainingSeconds = null
                ),
                    advanceToNextBlock = true)
            }
            else -> Rejected("cannot SKIP from ${current.state}")
        }

    // I don't need reset
    //private fun reset(current: TimerSnapshot, now: Instant): TransitionResult {}

    private fun expire(current: TimerSnapshot, now: Instant) : TransitionResult {

        val endAt =current.endAt ?: return NoOp(current)

        if (now.isBefore(endAt)) return NoOp(current)

        return when (current.state) {
            TimerState.WORK_RUNNING -> {
                val afterWork = current.copy(
                    state = TimerState.BREAK_RUNNING,
                    endAt = endAt.plusSeconds(current.breakSeconds.toLong()),
                    remainingSeconds = null,
                )
                when (val next = expire(afterWork, now)) {
                    is Applied -> next.copy(blockFinished = true)
                    else -> Applied(afterWork, blockFinished = true)
                }
            }
            TimerState.BREAK_RUNNING -> Applied(
                current.copy(state = TimerState.DONE, endAt = null),
                advanceToNextBlock = true,
            )
            else -> NoOp(current)
        }
    }
}
