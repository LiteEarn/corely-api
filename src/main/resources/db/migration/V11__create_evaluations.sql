CREATE TABLE evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    evaluation_date DATE NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    height DECIMAL(4,2) NOT NULL,
    observations VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluation_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE INDEX idx_evaluations_studio_id ON evaluations(studio_id);
CREATE INDEX idx_evaluations_student_id ON evaluations(student_id);
CREATE INDEX idx_evaluations_evaluation_date ON evaluations(evaluation_date);
