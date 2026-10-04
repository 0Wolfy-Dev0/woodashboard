-- V1 : core tables (users, events, todos)

CREATE TABLE users (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    login                  VARCHAR(64)  NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    role                   VARCHAR(16)  NOT NULL DEFAULT 'USER',
    creation_time          TIMESTAMP    NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_login UNIQUE (login),
    CONSTRAINT ck_users_role  CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE events (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    user_id                UUID         NOT NULL,
    start_time             TIMESTAMP    NOT NULL,
    end_time               TIMESTAMP    NOT NULL,
    creation_time          TIMESTAMP    NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_events_time CHECK (end_time >= start_time)
);

CREATE INDEX idx_events_user_start ON events (user_id, start_time);
CREATE INDEX idx_events_start      ON events (start_time);

CREATE TABLE todos (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    user_id                UUID         NOT NULL,
    completed              BOOLEAN      NOT NULL DEFAULT FALSE,
    creation_time          TIMESTAMP    NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_todos_user_completed ON todos (user_id, completed);
