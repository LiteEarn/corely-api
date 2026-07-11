ALTER TABLE class_groups
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE;

CREATE INDEX idx_class_group_start_date ON class_groups(start_date);
CREATE INDEX idx_class_group_end_date ON class_groups(end_date);
