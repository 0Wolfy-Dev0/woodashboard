-- V4 : personal todo folders and optional folder assignment for todos

CREATE TABLE todo_folders (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL,
    user_id                UUID         NOT NULL,
    creation_time          TIMESTAMP    NOT NULL DEFAULT now(),
    last_modification_time TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_todo_folders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_todo_folders_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_todo_folders_user ON todo_folders (user_id);

ALTER TABLE todos ADD COLUMN folder_id UUID;
ALTER TABLE todos ADD CONSTRAINT fk_todos_folder FOREIGN KEY (folder_id) REFERENCES todo_folders (id) ON DELETE SET NULL;
CREATE INDEX idx_todos_folder ON todos (folder_id);
