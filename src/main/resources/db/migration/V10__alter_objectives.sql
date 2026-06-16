-- V10__alter_objectives.sql

ALTER TABLE objectives ADD COLUMN studio_id UUID NOT NULL;
ALTER TABLE objectives ADD CONSTRAINT fk_objective_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE;

ALTER TABLE objectives ADD COLUMN title VARCHAR(100) NOT NULL;
ALTER TABLE objectives ALTER COLUMN description TYPE VARCHAR(500);
ALTER TABLE objectives ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE objectives ADD COLUMN target_date DATE;

ALTER TABLE objectives DROP COLUMN type;
ALTER TABLE objectives DROP COLUMN active;

CREATE INDEX idx_objective_studio_id ON objectives(studio_id);
CREATE INDEX idx_objective_status ON objectives(status);
DROP INDEX idx_objective_type;
