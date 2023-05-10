TRUNCATE TABLE data_tag;
TRUNCATE TABLE tagged_data_extend;
TRUNCATE TABLE tagged_data;

INSERT into data_tag
(DB_ID  	,TAG        , TAG_COUNT  	,HEIGHT , LATEST) VALUES
(10         ,'tag1'     ,      1         , 1500, TRUE),
(20         ,'tag2'     ,      2         , 2000, TRUE),
(30         ,'tag3'     ,      2         , 3500, TRUE),
(40         ,'imbatman' ,      1         , 3500, TRUE)
;

INSERT into tagged_data
(DB_ID  	,ID  	             , ACCOUNT_ID  	        , NAME  ,      description  ,tags                          , parsed_tags                 ,data              ,  is_text   ,  block_timestamp ,  transaction_timestamp , HEIGHT ) VALUES
(10         ,100 ,                    99  ,              'tag1'   , 'tag1 descr'    ,'tag1,tag2,tag3,tag2,sl'      , '["tag1", "tag2", "tag3"]'     ,X'c11dd7986e'     ,   TRUE    ,          18400    ,     1000        ,   2000 ),
(20         ,200,                     99  ,              'tag2'   , 'tag2 descr'    ,'tag2,tag2,ss'                , '["tag2"]'                     ,X'c11d86986e'     ,   TRUE    ,          32200    ,     2000        ,   3500 ),
(30         ,300 ,                    99  ,              'tag3'   , 'tag3 descr'    ,'tag3,tag4,tag3,newtag'       , '["tag3", "tag4", "newtag"]'   ,X'c11d8344588e'   ,   FALSE  ,          32200    ,      2000        ,   3500 ),
(40         ,400 ,                    99  ,              'tag4'   , 'tag4 descr'    ,'tag3,tag3,tag3,tag2,tag2'    , '["tag3", "tag2"]'             ,X'c11d1234589e'   ,   TRUE   ,          73600    ,      3000        ,   3500),
(50         ,500,                     99  ,              'tag5'   , 'tag5 descr'    ,'iambatman'                   , '["iambatman"]'                ,X'c11d1234586e'   ,   FALSE  ,          73600    ,      3000        ,   8000)
;