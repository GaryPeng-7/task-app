CREATE TABLE task_blocks (
    id                 uuid         not null  default gen_random_uuid()  primary key,
    user_id            uuid         not null  references users(id) on delete cascade,
    subject            text         null,
    description        text         null,
    block_date         date         not null,
    position           integer      not null,
    remaining_seconds  integer      null,
    status             varchar(20)  not null  default 'TO_BE_DONE'  check (status in ('TO_BE_DONE','IN_PROGRESS','FINISHED')),
    created_at         timestamptz  not null  default now(),
    updated_at         timestamptz  not null  default now()
);

CREATE INDEX idx_task_blocks_user_date_position
    ON task_blocks (user_id, block_date, position);
