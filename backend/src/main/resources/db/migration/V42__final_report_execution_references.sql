ALTER TABLE work_report ADD COLUMN execution_references jsonb NOT NULL DEFAULT '[]'::jsonb;
