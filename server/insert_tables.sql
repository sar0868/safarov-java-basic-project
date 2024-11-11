insert into users (id, login, password, username) values
(1, 'admin', 'admin', 'admin'),
(2, 'qwe', 'qwe', 'qwe1'),
(3,'asd','asd', 'asd1');

insert into roles (id, role) values
(1, 'ADMIN'),
(2, 'USER');

insert into users_to_roles (userid, roleid) values
(1,1),
(2,2),
(3,2);
