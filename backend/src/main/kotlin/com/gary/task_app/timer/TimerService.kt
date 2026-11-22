package com.gary.task_app.timer

import com.gary.task_app.taskblock.BlockStatus
import com.gary.task_app.taskblock.TaskBlock
import com.gary.task_app.taskblock.TaskBlockRepository
import com.gary.task_app.timer.dto.TimerStateResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.util.UUID

@Transactional
@Service
class TimerService(
    private val taskBlockRepository: TaskBlockRepository,
    private val timerRepository: TimerRepository,
    private val clock: Clock
) {

    fun start(blockId: UUID, userId: UUID)      = handleIntent(blockId, userId, TimerIntent.Start)
    fun pause(blockId: UUID, userId: UUID)      = handleIntent(blockId, userId, TimerIntent.Pause)
    fun resume(blockId: UUID, userId: UUID)     = handleIntent(blockId, userId, TimerIntent.Resume)
    fun complete(blockId: UUID, userId: UUID)   = handleIntent(blockId, userId, TimerIntent.Complete)
    fun skipBreak(blockId: UUID, userId: UUID)  = handleIntent(blockId, userId, TimerIntent.SkipBreak)

    private fun handleIntent(blockId: UUID, userId: UUID, intent: TimerIntent): TimerStateResponse {
        // 1. Load block via findByIdAndUserId(blockId, userId) -> 404 if null
        val block = taskBlockRepository.findByIdAndUserId(blockId, userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found")
        // 2. Load timer via timerRepository.findById(blockId) -> 404 if missing
        val timer = timerRepository.findById(blockId).orElseThrow{
            ResponseStatusException(HttpStatus.NOT_FOUND, "Timer not found")
        }
        // 3. Expiry first: CheckExpiry on the snapshot; if Applied -> apply(timer, block, expired)
        val expired = TimerStateMachine.transition(
            timer.toSnapshot(), TimerIntent.CheckExpiry, clock.instant()
        )
        if (expired is Applied) apply(timer, block, expired)
        // 4. Rebuild the snapshot, run transition(snapshot, intent, clock.instant())
        val result = TimerStateMachine.transition(
            timer.toSnapshot(), intent, clock.instant()
        )
        // 5. when (result):
        //      Applied  -> apply(...) and return TimerStateResponse.from(timer)
        //      NoOp     -> return TimerStateResponse.from(timer), no writes
        //      Rejected -> throw ResponseStatusException(CONFLICT, result.reason)
        return when (result) {
            is Applied -> {
                apply(timer, block, result)
                TimerStateResponse.from(timer)
            }
            is NoOp -> TimerStateResponse.from(timer)
            is Rejected -> throw ResponseStatusException(HttpStatus.CONFLICT, result.reason)
        }
    }

    fun getState(blockId: UUID, userId: UUID): TimerStateResponse {
        // 1. Same two loads + 404s (ownership check included)
        val block = taskBlockRepository.findByIdAndUserId(blockId, userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found")
        val timer = timerRepository.findById(blockId).orElseThrow{
            ResponseStatusException(HttpStatus.NOT_FOUND, "Timer not found")
        }
        // 2. Run CheckExpiry only — there is no user intent on a read
        val expired = TimerStateMachine.transition(
            timer.toSnapshot(), TimerIntent.CheckExpiry, clock.instant()
        )
        // 3. If Applied -> apply(timer, block, expired)
        if (expired is Applied) apply(timer, block, expired)
        //    This is the lazy-expiry money shot: a GET after 3 days silently
        //    finishes the block, burns the break, and promotes the next block
        // 4. Return TimerStateResponse.from(timer)
        return TimerStateResponse.from(timer)
    }

    private fun apply(timer: Timer, block: TaskBlock, result: Applied) {
        val snapshot = result.snapshot
        timer.timerState = snapshot.state
        timer.endsAt = snapshot.endAt
        timer.remainingSeconds = snapshot.remainingSeconds

        if (result.blockHasStarted) block.status = BlockStatus.IN_PROGRESS
        if (result.blockFinished) block.status = BlockStatus.FINISHED
        if (result.advanceToNextBlock) promoteNextBlock(block)
    }

    private fun promoteNextBlock(finished: TaskBlock) {
        val next = taskBlockRepository
            .findAllByUserIdAndBlockDateOrderByPositionAsc(finished.user.id,
                finished.blockDate)
            .firstOrNull { it.position > finished.position }
            ?: return

        next.status = BlockStatus.IN_PROGRESS
        if (!timerRepository.existsById(next.id)) {
            timerRepository.save(Timer(block = next))
        }
    }




    private fun Timer.toSnapshot() = TimerSnapshot(
        state = timerState,
        endAt = endsAt,
        remainingSeconds = remainingSeconds,
        workSeconds = workSeconds,
        breakSeconds = breakSeconds,
    )
}
