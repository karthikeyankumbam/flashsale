create extension if not exists pgcrypto;

create table orders (
  id uuid primary key default gen_random_uuid(),
  user_id varchar(64) not null,
  status varchar(32) not null,
  total_amount bigint not null,
  currency varchar(8) not null,
  idempotency_key varchar(128) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index uq_orders_user_idem
on orders(user_id, idempotency_key);

create table order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references orders(id) on delete cascade,
  sku varchar(128) not null,
  qty int not null,
  unit_price bigint not null
);

create table outbox_events (
  id uuid primary key default gen_random_uuid(),
  aggregate_type varchar(64) not null,
  aggregate_id uuid not null,
  event_type varchar(64) not null,
  payload_json text not null,
  status varchar(16) not null default 'NEW',
  created_at timestamptz not null default now()
);

create index idx_outbox_status_created
on outbox_events(status, created_at);