CREATE TABLE class_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    monday BOOLEAN NOT NULL DEFAULT FALSE,
    tuesday BOOLEAN NOT NULL DEFAULT FALSE,
    wednesday BOOLEAN NOT NULL DEFAULT FALSE,
    thursday BOOLEAN NOT NULL DEFAULT FALSE,
    friday BOOLEAN NOT NULL DEFAULT FALSE,
    saturday BOOLEAN NOT NULL DEFAULT FALSE,
    sunday BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_group_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_group_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE,
    CONSTRAINT chk_class_group_capacity CHECK (capacity > 0),
    CONSTRAINT chk_class_group_time CHECK (end_time > start_time),
    CONSTRAINT chk_class_group_at_least_one_day CHECK (
        monday = TRUE OR
        tuesday = TRUE OR
        wednesday = TRUE OR
        thursday = TRUE OR
        friday = TRUE OR
        saturday = TRUE OR
        sunday = TRUE
    )
);

CREATE INDEX idx_class_group_studio_id ON class_groups(studio_id);
CREATE INDEX idx_class_group_instructor_id ON class_groups(instructor_id);
CREATE INDEX idx_class_group_active ON class_groups(active);
CREATE INDEX idx_class_group_name ON class_groups(name);
