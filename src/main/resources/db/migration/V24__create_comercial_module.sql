-- Baseline migration for Módulo Comercial (ADR-001)
-- This migration marks the beginning of the commercial module's schema.
-- No functional domain tables are created yet — those will be introduced
-- in their respective future stories:
--   - Plan (replacing legacy plans table)
--   - StudentPlan (replacing legacy plan_enrollments table)
--   - Invoice
--   - Payment
-- All future commercial module migrations will use the V24+ sequence.
SELECT 1;
