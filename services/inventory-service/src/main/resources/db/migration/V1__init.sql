create extension if not exists pgcrypto;

create table inventory (
  sku varchar(128) primary key,
  available_qty int not null,
  reserved_qty int not null default 0,
  updated_at timestamptz not null default now()
);

create table reservations (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null unique,
  status varchar(16) not null,
  created_at timestamptz not null default now()
);

insert into inventory(sku, available_qty, reserved_qty)
values ('IPHONE-16-128-BLK', 10, 0)
on conflict (sku) do nothing;