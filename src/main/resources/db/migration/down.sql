-- Down Migration: Drop All Tables and Indexes

DROP TABLE IF EXISTS build_artifacts CASCADE;
DROP TABLE IF EXISTS build_logs CASCADE;
DROP TABLE IF EXISTS build_steps CASCADE;
DROP TABLE IF EXISTS builds CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
