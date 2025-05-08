create table author
(
    id         serial primary key,
    full_name  text not null,
    created_at timestamp not null
);

alter table budget add column author_id int references author(id) on delete set null;