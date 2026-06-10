CREATE TABLE system_login (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    date_created DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    auth_token_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_login_uuid UNIQUE (uuid),
    CONSTRAINT uk_system_login_email UNIQUE (email)
);

CREATE TABLE todo_project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    date_created DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    system_login_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_todo_project_uuid UNIQUE (uuid),
    CONSTRAINT fk_todo_project_login FOREIGN KEY (system_login_id)
        REFERENCES system_login(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_todo_project_login ON todo_project(system_login_id);

CREATE TABLE todo_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    date_created DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    todo_project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    sort_order DECIMAL(20,10) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_todo_task_uuid UNIQUE (uuid),
    CONSTRAINT fk_todo_task_project FOREIGN KEY (todo_project_id)
        REFERENCES todo_project(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_todo_task_project ON todo_task(todo_project_id);
CREATE INDEX idx_todo_task_project_status_order ON todo_task(todo_project_id, status, sort_order);
