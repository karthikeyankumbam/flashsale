alter table outbox_events
  add column if not exists last_error text,
  add column if not exists sent_at timestamptz;

-- optional: allow tracking attempts
alter table outbox_events
  add column if not exists attempt_count int not null default 0;