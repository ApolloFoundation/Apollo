delete from dex_operation;
INSERT INTO dex_operation
(db_id ,account                   , stage , eid     , description               , details, finished , ts        ) VALUES
(1001  ,'APL-RQTU-56W2-AAMY-7MTLB', 4     , 120     , 'Initiate atomic swap'    , null   , false    , '2020-01-10T12:00:01'),
(1002  ,'APL-EPHR-JVKK-2PWW-HPW9H', 4     , 120     , 'Initiate atomic swap'    , '100'  , true     , '2020-01-10T12:00:02'),
(1003  ,'APL-RQTU-56W2-AAMY-7MTLB', 4     , 130     , 'Initiate atomic swap'    , '100'  , false    , '2020-01-10T12:00:01'),
(1004  ,'APL-EPHR-JVKK-2PWW-HPW9H', 1     , 120     , 'New order'               , '100'  , true     , '2020-01-10T12:00:02'),
;