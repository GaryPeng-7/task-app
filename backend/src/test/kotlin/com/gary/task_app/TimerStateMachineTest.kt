package com.gary.task_app

import com.gary.task_app.timer.Applied
import com.gary.task_app.timer.NoOp
import com.gary.task_app.timer.Rejected
import com.gary.task_app.timer.TimerIntent
import com.gary.task_app.timer.TimerSnapshot
import com.gary.task_app.timer.TimerState
import com.gary.task_app.timer.TimerStateMachine
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimerStateMachineTest {
    private val t0 = Instant.parse("2026-10-07T10:00:00Z")

    @Test
    fun `start from IDLE begins work timer`() {
        val result = TimerStateMachine.transition(TimerSnapshot(TimerState.IDLE, workSeconds = 3000),
            TimerIntent.Start, t0)

        assertEquals(
            Applied(TimerSnapshot(TimerState.WORK_RUNNING,
                endAt = t0.plusSeconds(3000)), blockHasStarted = true),result
        )
    }

    @Test
    fun `pause while paused is a no-op`() {
        val result = TimerStateMachine.transition(TimerSnapshot(TimerState.WORK_PAUSED),
            TimerIntent.Pause, t0)

        assertEquals(
            NoOp(TimerSnapshot(TimerState.WORK_PAUSED)), result
        )
    }

    @Test
    fun `work expiry starts break from expiry instant, not discovery instant`() {
        val workEnd = t0.plusSeconds(3000)
        val snapshot = TimerSnapshot(
            state = TimerState.WORK_RUNNING,
            endAt = workEnd,
            workSeconds = 3000,
            breakSeconds = 300
        )
        val discovery = workEnd.plusSeconds(74)

        val result = TimerStateMachine.transition(snapshot,
            TimerIntent.CheckExpiry, discovery)

        assertEquals(
            Applied(
                TimerSnapshot(
                    state = TimerState.BREAK_RUNNING,
                    endAt = workEnd.plusSeconds(300),
                    workSeconds = 3000,
                    breakSeconds = 300,
                ),
                blockFinished = true,
            ),
            result
        )
    }

    @Test
    fun `expiry discovered 3 days late burns through work and break to DONE`() {
        val workEnd = t0.plusSeconds(3000)
        val snapshot = TimerSnapshot(
            state = TimerState.WORK_RUNNING,
            endAt = workEnd,
            workSeconds = 3000,
            breakSeconds = 300
        )
        val discovery = workEnd.plusSeconds(3 * 24 * 3600)

        val result = TimerStateMachine.transition(snapshot,
            TimerIntent.CheckExpiry, discovery)

        assertEquals(
            Applied(
                TimerSnapshot(
                    state = TimerState.DONE,
                    workSeconds = 3000,
                    breakSeconds = 300,
                ),
                blockFinished = true,
                advanceToNextBlock = true,
            ),
            result
        )
    }


    @ParameterizedTest
    @MethodSource("illegalIntents")
    fun `every illegal intent is rejected`(snapshot: TimerSnapshot, intent: TimerIntent) {
        val result = TimerStateMachine.transition(snapshot, intent, t0)
        assertTrue(
            result is Rejected,
            "expected Rejected for $intent in state ${snapshot.state}, but got $result",
        )
    }

    companion object {
        private val t = Instant.parse("2026-10-07T10:50:00Z")

        // Snapshots here must be LEGAL states — we're testing illegal INTENTS,
        // not illegal snapshots. Running timers get an endAt, paused get remaining.
        private fun running(state: TimerState) = TimerSnapshot(state, endAt = t)
        private fun paused(state: TimerState) = TimerSnapshot(state, remainingSeconds = 900)

        @JvmStatic
        fun illegalIntents() = listOf(
            // Start: legal from IDLE, NoOp from WORK_RUNNING — everything else rejects
            Arguments.of(paused(TimerState.WORK_PAUSED), TimerIntent.Start),
            Arguments.of(running(TimerState.BREAK_RUNNING), TimerIntent.Start),
            Arguments.of(paused(TimerState.BREAK_PAUSED), TimerIntent.Start),
            Arguments.of(TimerSnapshot(TimerState.DONE), TimerIntent.Start),

            // Pause: legal from RUNNING states, NoOp from PAUSED — IDLE/DONE reject
            Arguments.of(TimerSnapshot(TimerState.IDLE), TimerIntent.Pause),
            Arguments.of(TimerSnapshot(TimerState.DONE), TimerIntent.Pause),

            // Resume: legal from PAUSED states, NoOp from RUNNING — IDLE/DONE reject
            Arguments.of(TimerSnapshot(TimerState.IDLE), TimerIntent.Resume),
            Arguments.of(TimerSnapshot(TimerState.DONE), TimerIntent.Resume),

            // Complete: legal from WORK states only — breaks and endpoints reject
            Arguments.of(TimerSnapshot(TimerState.IDLE), TimerIntent.Complete),
            Arguments.of(running(TimerState.BREAK_RUNNING), TimerIntent.Complete),
            Arguments.of(paused(TimerState.BREAK_PAUSED), TimerIntent.Complete),
            Arguments.of(TimerSnapshot(TimerState.DONE), TimerIntent.Complete),

            // SkipBreak: legal from BREAK states only
            Arguments.of(TimerSnapshot(TimerState.IDLE), TimerIntent.SkipBreak),
            Arguments.of(running(TimerState.WORK_RUNNING), TimerIntent.SkipBreak),
            Arguments.of(paused(TimerState.WORK_PAUSED), TimerIntent.SkipBreak),
            Arguments.of(TimerSnapshot(TimerState.DONE), TimerIntent.SkipBreak),
        )
    }
}
