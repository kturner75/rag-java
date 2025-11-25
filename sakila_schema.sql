create table sakila.actor
(
    actor_id    smallint unsigned auto_increment
        primary key,
    first_name  varchar(45)                           not null,
    last_name   varchar(45)                           not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp()
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_actor_last_name
    on sakila.actor (last_name);

create table sakila.address
(
    address_id  smallint unsigned auto_increment
        primary key,
    address     varchar(50)                           not null,
    address2    varchar(50)                           null,
    district    varchar(20)                           not null,
    city_id     smallint unsigned                     not null,
    postal_code varchar(10)                           null,
    phone       varchar(20)                           not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp(),
    constraint fk_address_city
        foreign key (city_id) references sakila.city (city_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_city_id
    on sakila.address (city_id);

create table sakila.category
(
    category_id tinyint unsigned auto_increment
        primary key,
    name        varchar(25)                           not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp()
)
    collate = utf8mb4_uca1400_ai_ci;

create table sakila.city
(
    city_id     smallint unsigned auto_increment
        primary key,
    city        varchar(50)                           not null,
    country_id  smallint unsigned                     not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp(),
    constraint fk_city_country
        foreign key (country_id) references sakila.country (country_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_country_id
    on sakila.city (country_id);

create table sakila.country
(
    country_id  smallint unsigned auto_increment
        primary key,
    country     varchar(50)                           not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp()
)
    collate = utf8mb4_uca1400_ai_ci;

create table sakila.customer
(
    customer_id smallint unsigned auto_increment
        primary key,
    store_id    tinyint unsigned                       not null,
    first_name  varchar(45)                            not null,
    last_name   varchar(45)                            not null,
    email       varchar(50)                            null,
    address_id  smallint unsigned                      not null,
    active      tinyint(1) default 1                   not null,
    create_date datetime                               not null,
    last_update timestamp  default current_timestamp() null on update current_timestamp(),
    constraint fk_customer_address
        foreign key (address_id) references sakila.address (address_id)
            on update cascade,
    constraint fk_customer_store
        foreign key (store_id) references sakila.store (store_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_address_id
    on sakila.customer (address_id);

create index idx_fk_store_id
    on sakila.customer (store_id);

create index idx_last_name
    on sakila.customer (last_name);

create definer = root@localhost trigger sakila.customer_create_date
    before insert
    on sakila.customer
    for each row
    SET NEW.create_date = NOW();

create table sakila.film
(
    film_id              smallint unsigned auto_increment
        primary key,
    title                varchar(128)                                                            not null,
    description          varchar(255)                                                            null,
    release_year         int                                                                     not null,
    language_id          tinyint unsigned                                                        not null,
    original_language_id tinyint unsigned                                                        null,
    rental_duration      tinyint unsigned                        default 3                       not null,
    rental_rate          decimal(4, 2)                           default 4.99                    not null,
    length               smallint unsigned                                                       null,
    replacement_cost     decimal(5, 2)                           default 19.99                   not null,
    rating               enum ('G', 'PG', 'PG-13', 'R', 'NC-17') default 'G'                     null,
    special_features     set ('Trailers', 'Commentaries', 'Deleted Scenes', 'Behind the Scenes') null,
    last_update          timestamp                               default current_timestamp()     not null on update current_timestamp(),
    constraint fk_film_language
        foreign key (language_id) references sakila.language (language_id)
            on update cascade,
    constraint fk_film_language_original
        foreign key (original_language_id) references sakila.language (language_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_language_id
    on sakila.film (language_id);

create index idx_fk_original_language_id
    on sakila.film (original_language_id);

create index idx_title
    on sakila.film (title);

create definer = root@localhost trigger sakila.del_film
    after delete
    on sakila.film
    for each row
BEGIN
    DELETE FROM film_text WHERE film_id = old.film_id;
END;

create definer = root@localhost trigger sakila.ins_film
    after insert
    on sakila.film
    for each row
BEGIN
    INSERT INTO film_text (film_id, title, description)
    VALUES (new.film_id, new.title, new.description);
END;

create definer = root@localhost trigger sakila.upd_film
    after update
    on sakila.film
    for each row
BEGIN
    IF (old.title != new.title) OR (old.description != new.description) OR (old.film_id != new.film_id)
    THEN
        UPDATE film_text
        SET title=new.title,
            description=new.description,
            film_id=new.film_id
        WHERE film_id=old.film_id;
    END IF;
END;

create table sakila.film_actor
(
    actor_id    smallint unsigned                     not null,
    film_id     smallint unsigned                     not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp(),
    primary key (actor_id, film_id),
    constraint fk_film_actor_actor
        foreign key (actor_id) references sakila.actor (actor_id)
            on update cascade,
    constraint fk_film_actor_film
        foreign key (film_id) references sakila.film (film_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_film_id
    on sakila.film_actor (film_id);

create table sakila.film_category
(
    film_id     smallint unsigned                     not null,
    category_id tinyint unsigned                      not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp(),
    primary key (film_id, category_id),
    constraint fk_film_category_category
        foreign key (category_id) references sakila.category (category_id)
            on update cascade,
    constraint fk_film_category_film
        foreign key (film_id) references sakila.film (film_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create table sakila.film_text
(
    film_id     smallint unsigned not null
        primary key,
    title       varchar(255)      not null,
    description text              null
)
    collate = utf8mb4_uca1400_ai_ci;

create fulltext index idx_title_description
    on sakila.film_text (title, description);

create table sakila.inventory
(
    inventory_id mediumint unsigned auto_increment
        primary key,
    film_id      smallint unsigned                     not null,
    store_id     tinyint unsigned                      not null,
    last_update  timestamp default current_timestamp() not null on update current_timestamp(),
    constraint fk_inventory_film
        foreign key (film_id) references sakila.film (film_id)
            on update cascade,
    constraint fk_inventory_store
        foreign key (store_id) references sakila.store (store_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_film_id
    on sakila.inventory (film_id);

create index idx_store_id_film_id
    on sakila.inventory (store_id, film_id);

create table sakila.language
(
    language_id tinyint unsigned auto_increment
        primary key,
    name        char(20)                              not null,
    last_update timestamp default current_timestamp() not null on update current_timestamp()
)
    collate = utf8mb4_uca1400_ai_ci;

create table sakila.payment
(
    payment_id   smallint unsigned auto_increment
        primary key,
    customer_id  smallint unsigned                     not null,
    staff_id     tinyint unsigned                      not null,
    rental_id    int                                   null,
    amount       decimal(5, 2)                         not null,
    payment_date datetime                              not null,
    last_update  timestamp default current_timestamp() null on update current_timestamp(),
    constraint fk_payment_customer
        foreign key (customer_id) references sakila.customer (customer_id)
            on update cascade,
    constraint fk_payment_rental
        foreign key (rental_id) references sakila.rental (rental_id)
            on update cascade on delete set null,
    constraint fk_payment_staff
        foreign key (staff_id) references sakila.staff (staff_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_customer_id
    on sakila.payment (customer_id);

create index idx_fk_staff_id
    on sakila.payment (staff_id);

create definer = root@localhost trigger sakila.payment_date
    before insert
    on sakila.payment
    for each row
    SET NEW.payment_date = NOW();

create table sakila.rental
(
    rental_id    int auto_increment
        primary key,
    rental_date  datetime                              not null,
    inventory_id mediumint unsigned                    not null,
    customer_id  smallint unsigned                     not null,
    return_date  datetime                              null,
    staff_id     tinyint unsigned                      not null,
    last_update  timestamp default current_timestamp() not null on update current_timestamp(),
    constraint rental_date
        unique (rental_date, inventory_id, customer_id),
    constraint fk_rental_customer
        foreign key (customer_id) references sakila.customer (customer_id)
            on update cascade,
    constraint fk_rental_inventory
        foreign key (inventory_id) references sakila.inventory (inventory_id)
            on update cascade,
    constraint fk_rental_staff
        foreign key (staff_id) references sakila.staff (staff_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_customer_id
    on sakila.rental (customer_id);

create index idx_fk_inventory_id
    on sakila.rental (inventory_id);

create index idx_fk_staff_id
    on sakila.rental (staff_id);

create definer = root@localhost trigger sakila.rental_date
    before insert
    on sakila.rental
    for each row
    SET NEW.rental_date = NOW();

create table sakila.staff
(
    staff_id    tinyint unsigned auto_increment
        primary key,
    first_name  varchar(45)                            not null,
    last_name   varchar(45)                            not null,
    address_id  smallint unsigned                      not null,
    picture     blob                                   null,
    email       varchar(50)                            null,
    store_id    tinyint unsigned                       not null,
    active      tinyint(1) default 1                   not null,
    username    varchar(16)                            not null,
    password    varchar(40) collate utf8mb4_bin        null,
    last_update timestamp  default current_timestamp() not null on update current_timestamp(),
    constraint fk_staff_address
        foreign key (address_id) references sakila.address (address_id)
            on update cascade,
    constraint fk_staff_store
        foreign key (store_id) references sakila.store (store_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_address_id
    on sakila.staff (address_id);

create index idx_fk_store_id
    on sakila.staff (store_id);

create table sakila.store
(
    store_id         tinyint unsigned auto_increment
        primary key,
    manager_staff_id tinyint unsigned                      not null,
    address_id       smallint unsigned                     not null,
    last_update      timestamp default current_timestamp() not null on update current_timestamp(),
    constraint idx_unique_manager
        unique (manager_staff_id),
    constraint fk_store_address
        foreign key (address_id) references sakila.address (address_id)
            on update cascade,
    constraint fk_store_staff
        foreign key (manager_staff_id) references sakila.staff (staff_id)
            on update cascade
)
    collate = utf8mb4_uca1400_ai_ci;

create index idx_fk_address_id
    on sakila.store (address_id);
