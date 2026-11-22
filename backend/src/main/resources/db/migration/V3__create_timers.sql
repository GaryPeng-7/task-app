CREATE TABLE timers (
    block_id            uuid        primary key references task_blocks(id) on delete cascade,
    timer_state         varchar(20) not null default 'IDLE' check (timer_state in ('IDLE', 'WORK_RUNNING',
    'WORK_PAUSED', 'BREAK_RUNNING', 'BREAK_PAUSED', 'DONE')),
    ends_at             timestamptz null,
    remaining_seconds   integer     null,
    work_seconds        integer     not null default 3000,
    break_seconds       integer     not null default 300,

    check (
        (timer_state in ('WORK_RUNNING', 'BREAK_RUNNING')
            and ends_at is not null and remaining_seconds is null)
        or (timer_state in ('WORK_PAUSED', 'BREAK_PAUSED')
            and remaining_seconds is not null and ends_at is null)
        or (timer_state in ('IDLE', 'DONE')
            and ends_at is null and remaining_seconds is null)
        )
);

ALTER TABLE task_blocks DROP COLUMN remaining_seconds;
