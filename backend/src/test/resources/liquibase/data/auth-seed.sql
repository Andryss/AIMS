--liquibase formatted sql

--changeset andryss:seed-auth-data
insert into permission (code, description) values
    ('INCIDENT_READ', 'Read incidents'),
    ('INCIDENT_WRITE', 'Write incidents');

insert into role (name, description) values
    ('OPERATOR', 'Incident operator'),
    ('ADMIN', 'Administrator');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'OPERATOR' and p.code in ('INCIDENT_READ', 'INCIDENT_WRITE');

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r, permission p
where r.name = 'ADMIN' and p.code in ('INCIDENT_READ', 'INCIDENT_WRITE');

insert into app_user (login, password_hash) values
    ('agent', '$2a$10$LArEHlxdvPz42xMgLOLMLu2H9ZtkqH0Oge920nnSSL8Bowo4KJIKa');

insert into user_role (user_id, role_id)
select u.id, r.id
from app_user u, role r
where u.login = 'agent' and r.name = 'OPERATOR';
