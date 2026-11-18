alter table collections add column description varchar(400) not null default '';
create sequence collection_display_order start with 7;

create table portal_content (
  id integer primary key check (id = 1),
  hero_title varchar(180) not null,
  hero_description varchar(800) not null,
  community_title varchar(180) not null,
  community_description varchar(800) not null,
  about varchar(4000) not null,
  guardians varchar(4000) not null,
  contact_email varchar(254) not null,
  featured_memory bigint references memories
);

insert into portal_content values (
  1, 'Nossa história merece ser preservada.',
  'Um espaço digital para reunir, organizar e compartilhar fotografias, documentos, relatos e outras memórias que contam a história de Olinda Nova do Maranhão e de sua gente.',
  'Ajude a manter viva a memória da nossa cidade!',
  'Contribua com fotografias, documentos, relatos ou objetos que façam parte da história de Olinda Nova do Maranhão.',
  'Maranhão de Memórias é o Acervo Digital de Olinda Nova do Maranhão.',
  'Moradores, famílias e a comunidade escolar ajudam a identificar, compartilhar e preservar as memórias de Olinda Nova do Maranhão.',
  '', null
);
