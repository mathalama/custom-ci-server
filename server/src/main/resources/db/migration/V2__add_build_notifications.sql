ALTER TABLE builds ADD COLUMN author_email VARCHAR(255);
ALTER TABLE builds ADD COLUMN notify_on_success VARCHAR(1000);
ALTER TABLE builds ADD COLUMN notify_on_failure VARCHAR(1000);
