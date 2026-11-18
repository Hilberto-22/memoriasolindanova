create table city_history (
  id bigint generated always as identity primary key,
  title varchar(180) not null,
  category varchar(30) not null check (category in ('history', 'traditions', 'flag', 'anthem', 'other')),
  content text not null,
  period varchar(120) not null default '',
  start_date date,
  end_date date,
  source varchar(1000) not null default '',
  memory_id bigint references memories,
  check (end_date is null or (start_date is not null and end_date >= start_date))
);
