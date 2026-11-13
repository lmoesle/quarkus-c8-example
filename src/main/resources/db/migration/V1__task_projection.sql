CREATE TABLE task_projection (
    task_id text PRIMARY KEY,
    process_instance_id text NOT NULL,
    element_id text NOT NULL,
    repair_id text NOT NULL,
    display_name text NOT NULL,
    candidate_groups jsonb NOT NULL,
    payload jsonb NOT NULL
);
CREATE INDEX task_projection_repair ON task_projection(repair_id);
CREATE INDEX task_projection_element ON task_projection(element_id);
