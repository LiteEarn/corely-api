CREATE TABLE evolutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    objective_id UUID,
    evaluation_id UUID,
    evolution_date DATE NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evolution_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_evolution_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_evolution_objective FOREIGN KEY (objective_id) REFERENCES objectives(id) ON DELETE SET NULL,
    CONSTRAINT fk_evolution_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations(id) ON DELETE SET NULL
);

CREATE INDEX idx_evolutions_studio_id ON evolutions(studio_id);
CREATE INDEX idx_evolutions_student_id ON evolutions(student_id);
CREATE INDEX idx_evolutions_objective_id ON evolutions(objective_id);
CREATE INDEX idx_evolutions_evaluation_id ON evolutions(evaluation_id);
CREATE INDEX idx_evolutions_evolution_date ON evolutions(evolution_date);
