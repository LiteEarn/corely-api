-- V2__audit_log.sql
-- Trilha de auditoria LGPD (EPIC-02-S09)

CREATE TABLE corely.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES corely.users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_studio_id ON corely.audit_logs(studio_id);
CREATE INDEX idx_audit_log_user_id ON corely.audit_logs(user_id);
CREATE INDEX idx_audit_log_action ON corely.audit_logs(action);
CREATE INDEX idx_audit_log_occurred_at ON corely.audit_logs(occurred_at);
CREATE INDEX idx_audit_log_resource ON corely.audit_logs(resource_type, resource_id);
