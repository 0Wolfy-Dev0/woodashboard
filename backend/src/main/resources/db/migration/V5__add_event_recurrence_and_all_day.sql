-- V5 : all-day events and simple recurring series

ALTER TABLE events ADD COLUMN all_day BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN recurrence_unit VARCHAR(16);
ALTER TABLE events ADD COLUMN recurrence_interval INTEGER;
ALTER TABLE events ADD COLUMN recurrence_end TIMESTAMP;

ALTER TABLE events ADD CONSTRAINT ck_events_recurrence CHECK (
    (recurrence_unit IS NULL AND recurrence_interval IS NULL AND recurrence_end IS NULL)
    OR (recurrence_unit IN ('DAYS', 'WEEKS', 'MONTHS', 'YEARS') AND recurrence_interval >= 1)
);
ALTER TABLE events ADD CONSTRAINT ck_events_recurrence_end CHECK (
    recurrence_end IS NULL OR recurrence_end >= start_time
);
