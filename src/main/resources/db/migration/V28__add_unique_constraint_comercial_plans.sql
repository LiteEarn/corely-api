ALTER TABLE comercial_plans
ADD CONSTRAINT uq_comercial_plan_studio_name UNIQUE (studio_id, name);
