alter table collections add column cover_memory bigint references memories on delete set null;
