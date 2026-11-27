# Week 6 — Pomodoro State Sync

## Goal

The server owns the Pomodoro timer's authoritative state and time calculations. The frontend sends user intents and renders a countdown derived from server responses; it does not decide when a phase ends or persist the authoritative remaining time.

This design supports:

- Starting, pausing, resuming, completing, and skipping breaks.
- Correct countdown recovery after a hard refresh.
- Correct behavior after browser background-tab throttling.
- Pause/resume without extending or shortening the phase.
- Automatic work-to-break and break-to-next-block transitions when the timer is touched.
- State synchronization between connected clients through WebSocket transition broadcasts.
- Timer survival across backend restarts, subject to the PostgreSQL data remaining available.

## Core design decision: the server owns time

The client never sends a remaining-time value for the server to trust. It sends only intents such as `Start`, `Pause`, or `Resume`.

The server uses an injected `java.time.Clock` to obtain the current time. Persisted timestamps and durations determine the actual timer state:

- A **running** timer stores an absolute deadline in `ends_at`.
- A **paused** timer stores a frozen duration in `remaining_seconds`.
- An **idle** or **done** timer stores neither value.

The browser's countdown is only a continuously recomputed view. It is not a second authoritative timer.

### Why not decrement a database value every second?

A server tick-decrement design would require a reliable scheduled write every second for every active timer. Missed ticks, backend downtime, deployment restarts, and scaling to multiple application instances would make the value inaccurate.

Timestamp interpolation avoids that problem: elapsed time is derived from the difference between two timestamps, so no per-second database writes are necessary.

## Timer and block states are separate

A task block has a workflow state:

```kotlin
enum class BlockStatus {
    TO_BE_DONE,
    IN_PROGRESS,
    FINISHED,
}
```

A block's timer has a separate phase state:

```kotlin
enum class TimerState {
    IDLE,
    WORK_RUNNING,
    WORK_PAUSED,
    BREAK_RUNNING,
    BREAK_PAUSED,
    DONE,
}
```

`BlockStatus` describes the task's lifecycle. `TimerState` describes the Pomodoro clock. A block can therefore be `FINISHED` while its break timer is still running.

## Persistence model

Timer data is stored in the `timers` table separately from `task_blocks`.

Important columns:

| Column | Meaning |
|---|---|
| `block_id` | Primary key and foreign key to `task_blocks(id)` |
| `timer_state` | Current `TimerState`, stored as text |
| `ends_at` | Absolute deadline for a running phase |
| `remaining_seconds` | Frozen duration for a paused phase |
| `work_seconds` | Configured work duration for this timer |
| `break_seconds` | Configured break duration for this timer |

The foreign key uses `ON DELETE CASCADE`, so deleting a block also deletes its timer row.

The normal work/break sequence uses the persisted durations. The break is normally 300 seconds (five minutes).

### State invariants

| State | `ends_at` | `remaining_seconds` |
|---|---|---|
| `IDLE` | `NULL` | `NULL` |
| `WORK_RUNNING` | non-null | `NULL` |
| `WORK_PAUSED` | `NULL` | non-null |
| `BREAK_RUNNING` | non-null | `NULL` |
| `BREAK_PAUSED` | `NULL` | non-null |
| `DONE` | `NULL` | `NULL` |

These invariants prevent ambiguous states such as a paused timer that also has an active deadline.

Flyway migrations are immutable. Schema corrections are added as new migrations rather than editing a migration that has already been applied.

## Pure state machine

`TimerStateMachine` contains no Spring, JPA, repository, or WebSocket dependencies. Its input is the current snapshot, an intent, and the current instant. Its output describes the next timer state and any block-lifecycle effects the service must perform.

### Snapshot

```kotlin
data class TimerSnapshot(
    val state: TimerState,
    val endAt: Instant? = null,
    val remainingSeconds: Int? = null,
    val workSeconds: Int = 3000,
    val breakSeconds: Int = 300,
)
```

### Intents

```kotlin
sealed interface TimerIntent {
    data object Start : TimerIntent
    data object Pause : TimerIntent
    data object Resume : TimerIntent
    data object Complete : TimerIntent
    data object SkipBreak : TimerIntent
    data object CheckExpiry : TimerIntent
}
```

### Results

- `Applied`: a real transition occurred.
- `NoOp`: the request was idempotent or expiry was not yet due; the current state remains valid.
- `Rejected`: the intent is illegal in the current state.

`Applied` can also announce block-level effects:

- `blockHasStarted`
- `blockFinished`
- `advanceToNextBlock`

The pure machine does not modify JPA entities. `TimerService` executes those effects.

## Transition semantics

### Start

`IDLE -> WORK_RUNNING`

The deadline is calculated as:

```text
endAt = now + workSeconds
```

The service marks the block `IN_PROGRESS`.

Calling Start on an already running work timer is idempotent: it returns the existing state without moving the deadline.

### Pause

A running phase becomes its corresponding paused phase:

```text
WORK_RUNNING  -> WORK_PAUSED
BREAK_RUNNING -> BREAK_PAUSED
```

The frozen duration is calculated from the existing deadline:

```text
remainingSeconds = max(0, endsAt - now)
endAt = null
```

Pausing an already paused phase is a `NoOp`.

### Resume

A paused phase returns to its corresponding running phase:

```text
WORK_PAUSED  -> WORK_RUNNING
BREAK_PAUSED -> BREAK_RUNNING
```

The new deadline uses the frozen duration:

```text
endAt = now + remainingSeconds
remainingSeconds = null
```

Time spent paused is not included in the phase. Resuming an already running phase is a `NoOp`.

### Complete

Completing active or paused work produces:

```text
WORK_RUNNING or WORK_PAUSED -> BREAK_RUNNING
```

The block becomes `FINISHED`, and the break deadline is:

```text
endAt = now + breakSeconds
```

### Skip break

Skipping an active or paused break produces:

```text
BREAK_RUNNING or BREAK_PAUSED -> DONE
```

Both temporal fields become null. The service promotes the next block for the same user and date, ordered by position.

Promotion marks the next block `IN_PROGRESS` and ensures that it has a timer row, but it does not automatically start that timer.

## Expiry and long absences

Expiry is **lazy**. The server checks it when the timer is touched by:

- A `GET` state request.
- A timer intent such as Start, Pause, Resume, Complete, or Skip Break.

There is no requirement for a per-second scheduler.

### Before the deadline

If a running timer has not reached `endsAt`, `CheckExpiry` is a `NoOp`.

### Work expiry

When work has expired, the break is anchored to the original work deadline, not to the time at which the server happened to discover the expiry:

```text
breakEndsAt = oldWorkEndsAt + breakSeconds
```

This prevents time spent absent from being incorrectly added to the break.

The block is marked `FINISHED` as part of work expiry.

### Recursive expiry

After entering the break, the machine immediately checks whether that break has also expired.

For example, if someone returns after both a 50-minute work phase and its five-minute break have ended, the timer advances through both phases and finishes at `DONE`; it does not incorrectly display a fresh five-minute break.

Break expiry has the same final effect as Skip Break: the current timer becomes `DONE`, and the next block is promoted.

## Service transaction and error behavior

`TimerService` is transactional at its public entry points. This is necessary because Hibernate dirty checking must flush modified timer and block entities inside an active transaction.

Each intent follows the same general sequence:

1. Load the block using both block ID and user ID.
2. Return `404 Not Found` if that ownership-scoped lookup fails.
3. Load the timer or lazily create one for the block.
4. Run `CheckExpiry` and apply any resulting transition.
5. Rebuild the snapshot and process the requested intent.
6. Apply a successful transition, return the unchanged state for `NoOp`, or reject an illegal transition.
7. Return a DTO rather than exposing the JPA entity directly.

An illegal intent produces `409 Conflict` through `ResponseStatusException`. Because this is a runtime exception, the transaction rolls back. If the rejected request lazily created a timer before discovering the illegal transition, that new row is rolled back too. A rejected request therefore has no database side effects.

A read can write: `getState` may persist an expiry transition even though it is an HTTP GET.

## REST API

The timer endpoints are under:

```text
/api/blocks/{blockId}/timer
```

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/blocks/{blockId}/timer` | Read current state and apply lazy expiry |
| `POST` | `/api/blocks/{blockId}/timer/start` | Start work |
| `POST` | `/api/blocks/{blockId}/timer/pause` | Pause the current phase |
| `POST` | `/api/blocks/{blockId}/timer/resume` | Resume the paused phase |
| `POST` | `/api/blocks/{blockId}/timer/complete` | Finish work and start the break |
| `POST` | `/api/blocks/{blockId}/timer/skip` | Skip the break and promote the next block |

Intents do not require request bodies containing time values.

## Timer response DTO

The response includes the persisted timer information, the block ID, and a server-calculated remaining duration:

- `state`
- `endAt`
- `remainingSeconds`
- `serverRemainingSeconds`
- `workSeconds`
- `breakSeconds`
- `blockId`

`serverRemainingSeconds` is calculated by the server at response creation time:

- Running: `max(0, endsAt - serverNow)`.
- Paused: the frozen `remainingSeconds`.
- Idle or done: the configured work duration for display purposes.

Every HTTP response and WebSocket timer broadcast uses the same DTO construction path.

## WebSocket synchronization

Timer intents use HTTP. Successful changes are broadcast to the user-specific STOMP topic:

```text
/topic/users/{userId}/timer
```

The frontend sends HTTP responses and WebSocket messages through the same `setTimer` store function, so both sources update the same state.

### Broadcast transitions, not ticks

The server broadcasts when a transition occurs, such as Start, Pause, Resume, Complete, Skip Break, or an applied expiry. It does not broadcast a message every second.

Each connected client renders its own local interpolation between transition messages. This avoids unnecessary network traffic and prevents per-second broadcast timing from becoming part of the timer's correctness.

## Frontend countdown model

Every received timer response is stored with the browser timestamp `receivedAt`.

For a running timer, the displayed remaining time is approximately:

```text
serverRemainingSeconds
- max(0, floor((browserNow - receivedAt) / 1000))
```

The final value is clamped at zero.

For a paused timer, the displayed value is the frozen `remainingSeconds`.

### Why not subtract the browser time directly from `endAt`?

The server and browser clocks can differ. Directly evaluating `endAt - Date.now()` can therefore produce jumps or incorrect durations. `serverRemainingSeconds` anchors the duration to the server's clock, while `receivedAt` measures only elapsed display time on the client.

A one-second truncation difference is acceptable; multi-second pause/resume jumps are not.

### React behavior

- The one-second display interval remains mounted while running and paused so `now` cannot become stale.
- `now` is refreshed whenever a new timer response arrives.
- Negative elapsed-time calculations are clamped to zero.
- When a running countdown crosses zero, the frontend requests the authoritative state so lazy expiry can run.
- On `visibilitychange`, the frontend resynchronizes after background-tab throttling.
- A hard refresh fetches the current state from the server and reconstructs the countdown.

## Active timer UI

The large countdown belongs in the center `TimerPane`, not in every block row.

The active block is highlighted in the task list. A running or paused timer counts as active, so the UI hides other blocks' Start buttons while one timer is active.

This is currently a **UI-enforced** single-active-timer rule, not a backend invariant. A caller using the API directly may still be able to start multiple timers. Backend enforcement is a separate future improvement.

## Deleting a block with a timer

PostgreSQL removes the timer row through `ON DELETE CASCADE`, but that database event does not automatically remove the corresponding entry from the browser's Zustand store.

After a successful delete request, the frontend must therefore:

1. Remove the deleted block's timer from the timer store.
2. Refresh the block list.

`TimerPane` also ignores timer entries whose blocks no longer exist in the current block list. This prevents a deleted block's stale timer from occupying the center pane or hiding every other block's Start button.

Propagating block deletion to other open tabs is part of the later block-event synchronization work. A refresh still retrieves the authoritative server state.

## Backend restart semantics

The backend does not need to keep the timer in memory. Its authoritative state is in PostgreSQL.

| State before restart | Behavior after restart |
|---|---|
| Running, deadline not reached | The same absolute deadline remains valid; downtime counts as elapsed time |
| Running, work deadline passed | The next read or intent applies work expiry and starts the break from the original deadline |
| Running, work and break both expired | The next read or intent recursively reaches `DONE` and promotes the next block |
| Paused | The frozen remaining duration remains unchanged by downtime |
| Idle or done | The state remains unchanged |

Restarting the application does not itself reset a timer. Losing or resetting the PostgreSQL data is a separate operational event and is not covered by these guarantees.

Because expiry is lazy, a phase transition may not be persisted at the exact wall-clock deadline if no client touches the timer. The next touch brings the persisted state up to date.

## Testing strategy

### Pure state-machine tests

These tests require neither Spring nor a database. They cover:

- Start and idempotent Start.
- Pause and Resume calculations.
- Idempotent paused/running requests.
- Illegal intents.
- Work expiry into break.
- Break expiry into `DONE`.
- Recursive expiry after a long absence.
- Legal test fixtures that respect the timer invariants.

Test expectations must be based on the designed behavior, not copied from stale implementation defaults. Empty tests are not considered passing coverage.

### Service integration tests

`TimerServiceIntegrationTest` uses Testcontainers to run against a temporary PostgreSQL 16 database. Flyway creates the real schema, and the tests do not use the development database.

The suite covers:

1. Start persists the timer and marks the block `IN_PROGRESS`.
2. Repeated Start preserves the original deadline.
3. Pause and Resume preserve the remaining duration.
4. An illegal intent returns `409` and rolls back a lazily created timer.
5. Another user cannot operate the block's timer.
6. Complete and Skip Break finish the current block and promote the next one.
7. Pause after silent expiry cannot create negative remaining time.
8. `getState` recursively expires work and break after a long absence.

The test class itself is not transactional. Service calls commit independently, allowing the tests to verify persistence through later repository calls.

## Design decisions summary

- Persist timestamps and durations, not a value decremented every second.
- Use `endsAt` for running phases and `remainingSeconds` for paused phases.
- Anchor expiry to the original deadline rather than the discovery time.
- Process expiry lazily and recursively.
- Keep block workflow state separate from timer phase state.
- Keep the state machine pure and execute block side effects in the service.
- Return DTOs rather than JPA entities.
- Use HTTP for intents and WebSockets for transition broadcasts.
- Do not broadcast per-second ticks.
- Anchor the frontend countdown to `serverRemainingSeconds` and `receivedAt` to avoid server/client clock-skew jumps.
- Treat refresh, background-tab recovery, and backend restart as reconstruction from server state rather than restoration of a frontend timer.
- Remove deleted blocks' timer entries from frontend state; database cascade alone cannot update Zustand.

## Remaining work and limitations

- Enforce the single-active-timer rule in the backend if it becomes a required system invariant.
- Add block/reorder event broadcasts for complete multi-tab block-list synchronization.
- Complete the UUID-in-JWT refactor so controllers no longer resolve the authenticated email to a user ID through a per-request database lookup.
- Optionally persist chat messages; chat persistence is independent of timer correctness.
