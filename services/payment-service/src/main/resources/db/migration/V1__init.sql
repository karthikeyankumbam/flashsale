create extension if not exists pgcrypto;

create table payments (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null unique,
  amount bigint not null,
  currency varchar(8) not null,
  status varchar(16) not null, -- SUCCEEDED / FAILED
  failure_reason text,
  created_at timestamptz not null default now()
);