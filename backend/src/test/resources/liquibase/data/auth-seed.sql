--liquibase formatted sql

--changeset aims:seed-auth-data
insert into permission (code, description) values
    ('INCIDENT_READ', 'Read incidents'),
    ('INCIDENT_CREATE', 'Create incidents'),
    ('INCIDENT_STATUS_CHANGE', 'Change incident status');

insert into role (name, description) values
    ('OPERATOR', 'Incident operator'),
    ('ANALYST', 'Incident analyst'),
    ('ADMIN', 'Administrator');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'OPERATOR' and p.code in ('INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ANALYST' and p.code in ('INCIDENT_READ');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ADMIN' and p.code in ('INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE');

insert into app_user (login, password_hash) values
    ('operator', '$2a$10$3Lk4s75nd2Q0G1E8UWTmFu05efZd8lnw6OUw8yiJMvkN4fjhMKSfi'),
    ('analyst', '$2a$10$4jOz4wa0A4i9xlzxtJX5du72d2cbNfCjHt3TwPyDF6OlDXtIxK56W'),
    ('admin', '$2a$10$3ikPsa0.oG0Rolvk8W65Ke02QdCf1NguC9DxvAijlbu.54Vp/Wpb2');

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'operator' and r.name = 'OPERATOR';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'analyst' and r.name = 'ANALYST';

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'admin' and r.name = 'ADMIN';
