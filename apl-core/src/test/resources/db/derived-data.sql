create table if not exists derived_entity(db_id IDENTITY, id BIGINT NOT NULL, height INT NOT NULL);
delete from derived_entity;

insert into derived_entity
(db_id              ,id          ,height) VALUES
(1000               ,10          ,500   ),
(1010               ,20          ,500   ),
(1020               ,30          ,502   ),
(1030               ,40          ,506   ),
(1040               ,50          ,506   ),
(1050               ,60          ,506   ),
;
create table if not exists versioned_derived_entity(db_id IDENTITY, id BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL);
delete from versioned_derived_entity;
insert into versioned_derived_entity
(db_id    ,height          ,id     , latest) VALUES
(1000     , 99             , 1     , true ),
(1010     , 100            , 2     , false),
(1020     , 100            , 3     , false),
(1030     , 101            , 2     , false),
(1040     , 101            , 4     , false),
(1050     , 102            , 2     , true ),
(1060     , 102            , 4     , false),
(1070     , 105            , 3     , true ),
;

create table if not exists versioned_child_derived_entity(db_id IDENTITY, parent_id BIGINT NOT NULL, id BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL);
delete from versioned_child_derived_entity;
insert into versioned_child_derived_entity
(db_id    ,parent_id          ,id   ,height  , latest) VALUES
(1000     , 1                 , 1   , 125     , false),
(1010     , 2                 , 1   , 126     , false),
(1020     , 2                 , 1   , 127     , false),
(1030     , 2                 , 2   , 127     , false),
(1040     , 3                 , 1   , 127     , false),
(1050     , 1                 , 1   , 128     , true ),
(1060     , 1                 , 2   , 128     , true ),
(1070     , 2                 , 1   , 129     , true ),
(1080     , 2                 , 2   , 129     , true ),
(1090     , 2                 , 3   , 129     , true ),
(1100     , 3                 , 1   , 130     , false),
(1110     , 4                 , 1   , 130     , true ),
;
create table if not exists versioned_changeable_derived_entity(db_id IDENTITY, remaining BIGINT NOT NULL, id BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL);
delete from versioned_changeable_derived_entity;
insert into versioned_changeable_derived_entity
(db_id    ,id          ,remaining   ,height  , latest) VALUES
(1000     , 1          , 100        , 225    , false),
(1010     , 1          , 99         , 226    , false),
(1020     , 2          , 99         , 226    , false),
(1030     , 3          , 0          , 227    , true ),
(1040     , 4          , 0          , 227    , false),
(1050     , 2          , 10         , 228    , true ),
(1060     , 1          , 97         , 228    , true ),
(1070     , 4          , 0          , 228    , false),
;





