create table Users(
    id serial PRIMARY KEY,
    login varchar(100) not null,
    username varchar(100) not null,
    password varchar(100) not null
);


create table Roles(
	id serial primary key,
	role varchar(100)
);


create table Users_to_Roles(
	userID smallint not null,
	roleID smallint not null,
	primary key(userID, roleID),
	foreign key (userID) references Users(id) on delete cascade,
	foreign key (roleID) references Roles(id)
);


create table Groups(
    id serial primary key,
    title varchar(100) unique,
    adminID smallint not null,
    foreign key (adminID) references Users(id) on delete cascade
);


create table Users_to_Groups(
    userID smallint not null,
    groupID smallint not null,
    primary key(userID, groupID),
    foreign key (userID) references Users(id) on delete cascade,
    foreign key (groupID) references Groups(id) on delete cascade
);


create table Request_add_group (
	id serial primary key,
	userID smallint not null,
	groupID smallint not null,
	foreign key (userID) references Users(id) on delete cascade,
    foreign key (groupID) references Groups(id) on delete cascade
);

create table Messages(
	id serial primary key,
	userID smallint not null,
	groupID smallint not null,
	msg text not null,
	foreign key (userID) references Users(id) on delete cascade,
    foreign key (groupID) references Groups(id) on delete cascade
);

--============================================================






