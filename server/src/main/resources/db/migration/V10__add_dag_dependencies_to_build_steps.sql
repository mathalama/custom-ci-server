-- V10: Add DAG dependencies support to build_steps
ALTER TABLE build_steps ADD COLUMN depends_on TEXT DEFAULT '[]';
ALTER TABLE build_steps ADD COLUMN unresolved_dependencies_count INT DEFAULT 0;
