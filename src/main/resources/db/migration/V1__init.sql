-- V1__init.sql
-- Schema consolidado - Corely API

CREATE SCHEMA IF NOT EXISTS corely;

-- ============================================================
-- CORE MODULE
-- ============================================================

CREATE TABLE corely.studios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE corely.membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    monthly_price DECIMAL(10, 2) NOT NULL,
    sessions_per_week INTEGER NOT NULL CHECK (sessions_per_week > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_plan_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_membership_plan_studio_id ON corely.membership_plans(studio_id);
CREATE INDEX idx_membership_plan_active ON corely.membership_plans(active);

CREATE UNIQUE INDEX idx_membership_plan_unique_name_per_studio
    ON corely.membership_plans(studio_id, name)
    WHERE active = TRUE;

CREATE TABLE corely.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    studio_id UUID NOT NULL,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_email ON corely.users(email);
CREATE INDEX idx_user_studio_id ON corely.users(studio_id);
CREATE INDEX idx_user_role ON corely.users(role);

CREATE TABLE corely.students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    birth_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    billing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    membership_plan_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_membership_plan FOREIGN KEY (membership_plan_id) REFERENCES corely.membership_plans(id)
);

CREATE INDEX idx_student_studio_id ON corely.students(studio_id);
CREATE INDEX idx_student_email ON corely.students(email);
CREATE INDEX idx_student_full_name ON corely.students(full_name);

CREATE TABLE corely.instructors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    specialty VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_instructor_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_instructor_studio_id ON corely.instructors(studio_id);
CREATE INDEX idx_instructor_email ON corely.instructors(email);
CREATE INDEX idx_instructor_full_name ON corely.instructors(full_name);

CREATE TABLE corely.objectives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    studio_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    target_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_objective_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_objective_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_objective_student_id ON corely.objectives(student_id);
CREATE INDEX idx_objective_studio_id ON corely.objectives(studio_id);
CREATE INDEX idx_objective_status ON corely.objectives(status);

CREATE TABLE corely.evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    evaluation_date DATE NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    height DECIMAL(4,2) NOT NULL,
    observations VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluation_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE
);

CREATE INDEX idx_evaluations_studio_id ON corely.evaluations(studio_id);
CREATE INDEX idx_evaluations_student_id ON corely.evaluations(student_id);
CREATE INDEX idx_evaluations_evaluation_date ON corely.evaluations(evaluation_date);

CREATE TABLE corely.evolutions (
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
    CONSTRAINT fk_evolution_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_evolution_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_evolution_objective FOREIGN KEY (objective_id) REFERENCES corely.objectives(id) ON DELETE SET NULL,
    CONSTRAINT fk_evolution_evaluation FOREIGN KEY (evaluation_id) REFERENCES corely.evaluations(id) ON DELETE SET NULL
);

CREATE INDEX idx_evolutions_studio_id ON corely.evolutions(studio_id);
CREATE INDEX idx_evolutions_student_id ON corely.evolutions(student_id);
CREATE INDEX idx_evolutions_objective_id ON corely.evolutions(objective_id);
CREATE INDEX idx_evolutions_evaluation_id ON corely.evolutions(evaluation_id);
CREATE INDEX idx_evolutions_evolution_date ON corely.evolutions(evolution_date);

CREATE TABLE corely.class_groups (
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
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_group_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_group_instructor FOREIGN KEY (instructor_id) REFERENCES corely.instructors(id) ON DELETE CASCADE,
    CONSTRAINT chk_class_group_capacity CHECK (capacity > 0),
    CONSTRAINT chk_class_group_time CHECK (end_time > start_time),
    CONSTRAINT chk_class_group_at_least_one_day CHECK (
        monday = TRUE OR tuesday = TRUE OR wednesday = TRUE OR
        thursday = TRUE OR friday = TRUE OR saturday = TRUE OR sunday = TRUE
    )
);

CREATE INDEX idx_class_group_studio_id ON corely.class_groups(studio_id);
CREATE INDEX idx_class_group_instructor_id ON corely.class_groups(instructor_id);
CREATE INDEX idx_class_group_active ON corely.class_groups(active);
CREATE INDEX idx_class_group_name ON corely.class_groups(name);
CREATE INDEX idx_class_group_start_date ON corely.class_groups(start_date);
CREATE INDEX idx_class_group_end_date ON corely.class_groups(end_date);

CREATE TABLE corely.class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_group_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    cancel_reason VARCHAR(30),
    cancel_description VARCHAR(500),
    cancelled_by UUID,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_session_class_group FOREIGN KEY (class_group_id) REFERENCES corely.class_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_session_instructor FOREIGN KEY (instructor_id) REFERENCES corely.instructors(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_session_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES corely.users(id),
    CONSTRAINT uq_class_session_group_date UNIQUE (class_group_id, session_date)
);

CREATE INDEX idx_class_session_class_group_id ON corely.class_sessions(class_group_id);
CREATE INDEX idx_class_session_session_date ON corely.class_sessions(session_date);
CREATE INDEX idx_class_session_status ON corely.class_sessions(status);

CREATE TABLE corely.enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancel_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_class_group FOREIGN KEY (class_group_id) REFERENCES corely.class_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_enrollment_student_class_group UNIQUE (student_id, class_group_id)
);

CREATE INDEX idx_enrollment_studio_id ON corely.enrollments(studio_id);
CREATE INDEX idx_enrollment_student_id ON corely.enrollments(student_id);
CREATE INDEX idx_enrollment_class_group_id ON corely.enrollments(class_group_id);
CREATE INDEX idx_enrollment_active ON corely.enrollments(active);
CREATE INDEX idx_enrollment_enrollment_date ON corely.enrollments(enrollment_date);

CREATE TABLE corely.attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_session_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_class_session FOREIGN KEY (class_session_id) REFERENCES corely.class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES corely.enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_session_enrollment UNIQUE (class_session_id, enrollment_id)
);

CREATE INDEX idx_attendance_class_session_id ON corely.attendances(class_session_id);
CREATE INDEX idx_attendance_enrollment_id ON corely.attendances(enrollment_id);
CREATE INDEX idx_attendance_status ON corely.attendances(status);

CREATE TABLE corely.makeup_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_id UUID NOT NULL,
    target_session_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(500),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_makeup_request_attendance FOREIGN KEY (attendance_id) REFERENCES corely.attendances(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_request_target_session FOREIGN KEY (target_session_id) REFERENCES corely.class_sessions(id) ON DELETE SET NULL,
    CONSTRAINT uq_makeup_request_attendance UNIQUE (attendance_id)
);

CREATE INDEX idx_makeup_request_attendance_id ON corely.makeup_requests(attendance_id);
CREATE INDEX idx_makeup_request_target_session_id ON corely.makeup_requests(target_session_id);
CREATE INDEX idx_makeup_request_status ON corely.makeup_requests(status);

CREATE TABLE corely.makeup_eligibility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ELIGIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_makeup_eligibility_session FOREIGN KEY (session_id) REFERENCES corely.class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_enrollment FOREIGN KEY (enrollment_id) REFERENCES corely.enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_class_group FOREIGN KEY (class_group_id) REFERENCES corely.class_groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_makeup_eligibility_session_id ON corely.makeup_eligibility(session_id);
CREATE INDEX idx_makeup_eligibility_student_id ON corely.makeup_eligibility(student_id);
CREATE INDEX idx_makeup_eligibility_status ON corely.makeup_eligibility(status);

CREATE TABLE corely.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES corely.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_token ON corely.refresh_tokens(token);
CREATE INDEX idx_refresh_token_user_id ON corely.refresh_tokens(user_id);

-- ============================================================
-- COMERCIAL MODULE
-- ============================================================

CREATE TABLE corely.comercial_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    duration INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_plan_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT chk_comercial_plan_price CHECK (price > 0),
    CONSTRAINT chk_comercial_plan_duration CHECK (duration > 0),
    CONSTRAINT uq_comercial_plan_studio_name UNIQUE (studio_id, name)
);

CREATE INDEX idx_comercial_plan_studio_id ON corely.comercial_plans(studio_id);
CREATE INDEX idx_comercial_plan_active ON corely.comercial_plans(active);
CREATE INDEX idx_comercial_plan_name ON corely.comercial_plans(name);

CREATE TABLE corely.comercial_rule_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    value_type VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comercial_rule_definition_code UNIQUE (code),
    CONSTRAINT chk_comercial_rule_definition_value_type CHECK (value_type IN ('BOOLEAN', 'INTEGER', 'DECIMAL', 'STRING', 'ENUM')),
    CONSTRAINT chk_comercial_rule_definition_category CHECK (category IN ('VALIDITY', 'ATTENDANCE', 'BILLING', 'BOOKING', 'CANCELLATION', 'GENERAL'))
);

CREATE INDEX idx_comercial_rule_definition_active ON corely.comercial_rule_definitions(active);
CREATE INDEX idx_comercial_rule_definition_code ON corely.comercial_rule_definitions(code);
CREATE INDEX idx_comercial_rule_definition_category ON corely.comercial_rule_definitions(category);

CREATE TABLE corely.comercial_plan_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    rule_definition_id UUID NOT NULL,
    rule_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_plan_rule_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_plan_rule_plan FOREIGN KEY (plan_id) REFERENCES corely.comercial_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_plan_rule_rule_definition FOREIGN KEY (rule_definition_id) REFERENCES corely.comercial_rule_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_plan_rule_plan_rule_def UNIQUE (plan_id, rule_definition_id)
);

CREATE INDEX idx_comercial_plan_rule_studio_id ON corely.comercial_plan_rules(studio_id);
CREATE INDEX idx_comercial_plan_rule_plan_id ON corely.comercial_plan_rules(plan_id);
CREATE INDEX idx_comercial_plan_rule_rule_definition_id ON corely.comercial_plan_rules(rule_definition_id);

CREATE TABLE corely.comercial_contract_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_version INTEGER NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    plan_description TEXT,
    plan_price DECIMAL(10, 2) NOT NULL,
    plan_duration INTEGER NOT NULL,
    rules TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comercial_contract_snapshot_studio_id ON corely.comercial_contract_snapshots(studio_id);
CREATE INDEX idx_comercial_contract_snapshot_plan_id ON corely.comercial_contract_snapshots(plan_id);
CREATE INDEX idx_comercial_contract_snapshot_created_at ON corely.comercial_contract_snapshots(created_at);

CREATE TABLE corely.comercial_student_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    contract_snapshot_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancellation_date DATE,
    cancellation_reason VARCHAR(500),
    booking_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    suspension_reason VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_student_plan_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_student_plan_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_student_plan_snapshot FOREIGN KEY (contract_snapshot_id) REFERENCES corely.comercial_contract_snapshots(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_student_plan_active_per_student UNIQUE (student_id, status)
);

CREATE INDEX idx_comercial_student_plan_studio_id ON corely.comercial_student_plans(studio_id);
CREATE INDEX idx_comercial_student_plan_student_id ON corely.comercial_student_plans(student_id);
CREATE INDEX idx_comercial_student_plan_status ON corely.comercial_student_plans(status);
CREATE INDEX idx_comercial_student_plan_start_date ON corely.comercial_student_plans(start_date);

CREATE TABLE corely.comercial_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_plan_id UUID NOT NULL,
    due_date DATE NOT NULL,
    reference_month VARCHAR(7) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_invoice_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_invoice_student_plan FOREIGN KEY (student_plan_id) REFERENCES corely.comercial_student_plans(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_invoice_student_plan_month UNIQUE (student_plan_id, reference_month)
);

CREATE INDEX idx_comercial_invoice_studio_id ON corely.comercial_invoices(studio_id);
CREATE INDEX idx_comercial_invoice_student_plan_id ON corely.comercial_invoices(student_plan_id);
CREATE INDEX idx_comercial_invoice_due_date ON corely.comercial_invoices(due_date);
CREATE INDEX idx_comercial_invoice_status ON corely.comercial_invoices(status);
CREATE INDEX idx_comercial_invoice_reference_month ON corely.comercial_invoices(reference_month);

CREATE TABLE corely.comercial_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    external_reference VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_payment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_payment_invoice FOREIGN KEY (invoice_id) REFERENCES corely.comercial_invoices(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_payment_invoice UNIQUE (invoice_id)
);

CREATE INDEX idx_comercial_payment_studio_id ON corely.comercial_payments(studio_id);
CREATE INDEX idx_comercial_payment_invoice_id ON corely.comercial_payments(invoice_id);
CREATE INDEX idx_comercial_payment_payment_date ON corely.comercial_payments(payment_date);
CREATE INDEX idx_comercial_payment_payment_method ON corely.comercial_payments(payment_method);

CREATE TABLE corely.comercial_billing_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_plan_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    billing_day INTEGER NOT NULL,
    next_billing_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_billing_schedule_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_billing_schedule_student_plan FOREIGN KEY (student_plan_id) REFERENCES corely.comercial_student_plans(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_billing_schedule_student_plan UNIQUE (student_plan_id),
    CONSTRAINT chk_comercial_billing_schedule_billing_day CHECK (billing_day >= 1 AND billing_day <= 31)
);

CREATE INDEX idx_comercial_billing_schedule_studio_id ON corely.comercial_billing_schedules(studio_id);
CREATE INDEX idx_comercial_billing_schedule_next_billing_date ON corely.comercial_billing_schedules(next_billing_date);
CREATE INDEX idx_comercial_billing_schedule_active ON corely.comercial_billing_schedules(active);
CREATE INDEX idx_comercial_billing_schedule_frequency ON corely.comercial_billing_schedules(frequency);

CREATE TABLE corely.comercial_delinquency_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    grace_period_days INTEGER NOT NULL DEFAULT 0,
    action VARCHAR(30) NOT NULL DEFAULT 'NONE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_delinquency_policy_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_delinquency_policy_studio UNIQUE (studio_id),
    CONSTRAINT chk_comercial_delinquency_policy_grace_period CHECK (grace_period_days >= 0)
);

CREATE INDEX idx_comercial_delinquency_policy_studio_id ON corely.comercial_delinquency_policies(studio_id);
CREATE INDEX idx_comercial_delinquency_policy_active ON corely.comercial_delinquency_policies(active);

CREATE TABLE corely.comercial_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_schedule_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_schedule_studio_name UNIQUE (studio_id, name)
);

CREATE INDEX idx_comercial_schedule_studio_id ON corely.comercial_schedules(studio_id);

CREATE TABLE corely.comercial_schedule_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    instructor_id UUID,
    room_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_schedule_slot_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_schedule_slot_schedule FOREIGN KEY (schedule_id) REFERENCES corely.comercial_schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_schedule_slot_instructor FOREIGN KEY (instructor_id) REFERENCES corely.instructors(id),
    CONSTRAINT ck_comercial_schedule_slot_time CHECK (end_time > start_time),
    CONSTRAINT ck_comercial_schedule_slot_capacity CHECK (capacity > 0)
);

CREATE INDEX idx_comercial_schedule_slot_schedule ON corely.comercial_schedule_slots(schedule_id);
CREATE INDEX idx_comercial_schedule_slot_schedule_day ON corely.comercial_schedule_slots(schedule_id, day_of_week);
CREATE INDEX idx_comercial_schedule_slot_instructor ON corely.comercial_schedule_slots(instructor_id);

CREATE TABLE corely.comercial_class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    schedule_slot_id UUID NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    booked_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    cancel_reason VARCHAR(30),
    cancel_description TEXT,
    cancelled_by UUID,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_class_session_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_class_session_slot FOREIGN KEY (schedule_slot_id) REFERENCES corely.comercial_schedule_slots(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_class_session_slot_date UNIQUE (schedule_slot_id, session_date),
    CONSTRAINT ck_comercial_class_session_time CHECK (end_time > start_time)
);

CREATE INDEX idx_comercial_class_session_slot ON corely.comercial_class_sessions(schedule_slot_id);
CREATE INDEX idx_comercial_class_session_date ON corely.comercial_class_sessions(session_date);
CREATE INDEX idx_comercial_class_session_status ON corely.comercial_class_sessions(status);

CREATE TABLE corely.comercial_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    class_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    booking_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    cancel_reason VARCHAR(20),
    cancel_description TEXT,
    cancelled_by UUID,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_booking_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_booking_class_session FOREIGN KEY (class_session_id) REFERENCES corely.comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_booking_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_booking_class_session_student UNIQUE (class_session_id, student_id)
);

CREATE INDEX idx_comercial_booking_class_session ON corely.comercial_bookings(class_session_id);
CREATE INDEX idx_comercial_booking_student ON corely.comercial_bookings(student_id);
CREATE INDEX idx_comercial_booking_status ON corely.comercial_bookings(status);

CREATE TABLE corely.comercial_attendances (
    id UUID NOT NULL,
    studio_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    checked_in_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_comercial_attendances PRIMARY KEY (id),
    CONSTRAINT fk_comercial_attendances_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id),
    CONSTRAINT fk_comercial_attendances_booking FOREIGN KEY (booking_id) REFERENCES corely.comercial_bookings(id),
    CONSTRAINT uq_comercial_attendances_booking UNIQUE (booking_id)
);

CREATE INDEX idx_comercial_attendances_booking_id ON corely.comercial_attendances(booking_id);
CREATE INDEX idx_comercial_attendances_student_id ON corely.comercial_attendances(studio_id);

CREATE TABLE corely.comercial_wait_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    class_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    position INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_wait_list_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_wait_list_class_session FOREIGN KEY (class_session_id) REFERENCES corely.comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_wait_list_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_wait_list_session_student UNIQUE (class_session_id, student_id, active)
);

CREATE INDEX idx_comercial_wait_list_class_session ON corely.comercial_wait_list(class_session_id);
CREATE INDEX idx_comercial_wait_list_student ON corely.comercial_wait_list(student_id);
CREATE INDEX idx_comercial_wait_list_status ON corely.comercial_wait_list(status);
CREATE INDEX idx_comercial_wait_list_position ON corely.comercial_wait_list(class_session_id, position);

CREATE TABLE corely.comercial_makeup_credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    original_attendance_id UUID NOT NULL,
    original_class_session_id UUID NOT NULL,
    makeup_booking_id UUID,
    expiration_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_makeup_credits_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_original_attendance FOREIGN KEY (original_attendance_id) REFERENCES corely.comercial_attendances(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_original_class_session FOREIGN KEY (original_class_session_id) REFERENCES corely.comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_makeup_booking FOREIGN KEY (makeup_booking_id) REFERENCES corely.comercial_bookings(id) ON DELETE SET NULL
);

CREATE INDEX idx_comercial_makeup_credits_student ON corely.comercial_makeup_credits(student_id);
CREATE INDEX idx_comercial_makeup_credits_status ON corely.comercial_makeup_credits(status);
CREATE INDEX idx_comercial_makeup_credits_expiration_date ON corely.comercial_makeup_credits(expiration_date);

CREATE TABLE corely.comercial_time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID,
    room_id BIGINT,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    reason TEXT,
    block_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_time_block_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_time_block_instructor FOREIGN KEY (instructor_id) REFERENCES corely.instructors(id) ON DELETE CASCADE,
    CONSTRAINT ck_comercial_time_block_range CHECK (end_date_time > start_date_time)
);

CREATE INDEX idx_comercial_time_block_studio ON corely.comercial_time_blocks(studio_id);
CREATE INDEX idx_comercial_time_block_instructor ON corely.comercial_time_blocks(instructor_id);
CREATE INDEX idx_comercial_time_block_dates ON corely.comercial_time_blocks(start_date_time, end_date_time);

-- ============================================================
-- FINANCE MODULE
-- ============================================================

CREATE TABLE corely.finance_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date DATE,
    billing_month VARCHAR(7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_finance_invoice_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_finance_invoice_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE
);

CREATE INDEX idx_finance_invoice_student_id ON corely.finance_invoices(student_id);
CREATE INDEX idx_finance_invoice_status ON corely.finance_invoices(status);
CREATE INDEX idx_finance_invoice_due_date ON corely.finance_invoices(due_date);
CREATE INDEX idx_finance_invoice_billing_month ON corely.finance_invoices(billing_month);

CREATE UNIQUE INDEX idx_finance_invoice_unique_student_month
    ON corely.finance_invoices(student_id, billing_month, studio_id)
    WHERE billing_month IS NOT NULL;

CREATE TABLE corely.billing_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL UNIQUE,
    due_day INTEGER NOT NULL CHECK (due_day >= 1 AND due_day <= 31),
    default_amount DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_billing_configuration_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_billing_configuration_studio_id ON corely.billing_configurations(studio_id);

-- ============================================================
-- BOOKING MODULE
-- ============================================================

CREATE TABLE corely.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    student_id UUID NOT NULL,
    room_id BIGINT,
    class_type VARCHAR(100) NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    capacity INTEGER,
    make_up_class BOOLEAN NOT NULL DEFAULT FALSE,
    original_booking_id UUID,
    cancellation_reason VARCHAR(20),
    cancellation_notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_instructor FOREIGN KEY (instructor_id) REFERENCES corely.instructors(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_original FOREIGN KEY (original_booking_id) REFERENCES corely.bookings(id) ON DELETE SET NULL,
    CONSTRAINT chk_booking_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_booking_cancel_reason CHECK (cancellation_reason IN ('STUDENT', 'STUDIO', 'INSTRUCTOR', 'WEATHER', 'OTHER')),
    CONSTRAINT chk_booking_time CHECK (end_date_time > start_date_time)
);

CREATE INDEX idx_booking_studio_id ON corely.bookings(studio_id);
CREATE INDEX idx_booking_instructor_id ON corely.bookings(instructor_id);
CREATE INDEX idx_booking_student_id ON corely.bookings(student_id);
CREATE INDEX idx_booking_room_id ON corely.bookings(room_id);
CREATE INDEX idx_booking_start_date_time ON corely.bookings(start_date_time);
CREATE INDEX idx_booking_status ON corely.bookings(status);
CREATE INDEX idx_booking_active ON corely.bookings(active);

CREATE TABLE corely.time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id BIGINT,
    room_id BIGINT,
    block_type VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_block_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT chk_block_type CHECK (block_type IN ('INSTRUCTOR_VACATION', 'ROOM_MAINTENANCE', 'HOLIDAY', 'ADMINISTRATIVE')),
    CONSTRAINT chk_block_time CHECK (end_date > start_date)
);

CREATE INDEX idx_time_block_studio_id ON corely.time_blocks(studio_id);
CREATE INDEX idx_time_block_instructor_id ON corely.time_blocks(instructor_id);
CREATE INDEX idx_time_block_room_id ON corely.time_blocks(room_id);
CREATE INDEX idx_time_block_start_date ON corely.time_blocks(start_date);
CREATE INDEX idx_time_block_end_date ON corely.time_blocks(end_date);
