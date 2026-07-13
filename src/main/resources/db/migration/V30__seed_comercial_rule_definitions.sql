-- Seed oficial de RuleDefinitions do Corely
-- Idempotente: cada INSERT verifica se o code já existe

-- VALIDITY
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'VALIDITY_DAYS', 'Validity Days', 'Number of days the plan remains valid', 'INTEGER', 'VALIDITY', true, '30', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'VALIDITY_DAYS');

-- ATTENDANCE
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'MAX_CLASSES', 'Maximum Classes', 'Maximum number of classes per cycle', 'INTEGER', 'ATTENDANCE', true, '0', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'MAX_CLASSES');

-- BOOKING
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'MAX_FUTURE_BOOKINGS', 'Maximum Future Bookings', 'Maximum number of future bookings allowed', 'INTEGER', 'BOOKING', true, '10', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'MAX_FUTURE_BOOKINGS');

INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'DAILY_LIMIT', 'Daily Booking Limit', 'Maximum number of bookings allowed per day', 'INTEGER', 'BOOKING', true, '1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'DAILY_LIMIT');

-- CANCELLATION
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'ALLOW_MAKEUP', 'Allow Makeup Classes', 'Allow makeup classes for cancelled sessions', 'BOOLEAN', 'CANCELLATION', true, 'true', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'ALLOW_MAKEUP');

INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'MAKEUP_VALIDITY_DAYS', 'Makeup Validity Days', 'Number of days to use makeup classes', 'INTEGER', 'CANCELLATION', true, '30', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'MAKEUP_VALIDITY_DAYS');

-- BILLING
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'AUTO_RENEW', 'Auto Renew', 'Automatically renew the plan at the end of the cycle', 'BOOLEAN', 'BILLING', true, 'true', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'AUTO_RENEW');

INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'BILLING_CYCLE', 'Billing Cycle', 'Billing cycle frequency (MONTHLY, BIMONTHLY, QUARTERLY, SEMESTERLY, YEARLY)', 'STRING', 'BILLING', true, 'MONTHLY', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'BILLING_CYCLE');

INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'GRACE_PERIOD_DAYS', 'Grace Period Days', 'Number of days to pay before plan suspension', 'INTEGER', 'BILLING', true, '3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'GRACE_PERIOD_DAYS');

-- GENERAL
INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'ACTIVE_ON_PAYMENT', 'Active on Payment', 'Activate the plan upon first payment confirmation', 'BOOLEAN', 'GENERAL', true, 'true', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'ACTIVE_ON_PAYMENT');

INSERT INTO comercial_rule_definitions (id, code, name, description, value_type, category, required, default_value, active, created_at, updated_at)
SELECT gen_random_uuid(), 'ALLOW_OVERDUE_BOOKING', 'Allow Overdue Booking', 'Allow booking classes when plan is overdue', 'BOOLEAN', 'GENERAL', true, 'false', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM comercial_rule_definitions WHERE code = 'ALLOW_OVERDUE_BOOKING');
