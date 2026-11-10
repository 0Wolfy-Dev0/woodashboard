-- V3 : groups and optional group sharing for events/todos

CREATE TABLE groups (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL,
    description            TEXT,
    owner_id               UUID         NOT NULL,
    creation_time          TIMESTAMP    NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_groups_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_groups_owner ON groups (owner_id);

CREATE TABLE group_members (
    id                     UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id               UUID      NOT NULL,
    user_id                UUID      NOT NULL,
    creation_time          TIMESTAMP NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT uq_group_members_pair  UNIQUE (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members (user_id);

ALTER TABLE events ADD COLUMN group_id UUID;
ALTER TABLE events ADD CONSTRAINT fk_events_group FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE;
CREATE INDEX idx_events_group_start ON events (group_id, start_time);

ALTER TABLE todos ADD COLUMN group_id UUID;
ALTER TABLE todos ADD CONSTRAINT fk_todos_group FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE;
CREATE INDEX idx_todos_group_completed ON todos (group_id, completed);
