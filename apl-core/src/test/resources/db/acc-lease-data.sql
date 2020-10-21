DELETE FROM account;
DELETE FROM account_asset;
DELETE FROM account_currency;
DELETE FROM account_guaranteed_balance;
--DELETE FROM FTL.INDEXES;
DELETE FROM account_ledger;
DELETE FROM account_property;
DELETE FROM account_lease;


INSERT INTO account
(DB_ID  ,ID      ,BALANCE  	             ,UNCONFIRMED_BALANCE  	    ,HAS_CONTROL_PHASING  	,FORGED_BALANCE  	,ACTIVE_LESSEE_ID  	,HEIGHT  	 ,LATEST) values
(1       ,10     ,999990000000000     ,999990000000000       ,false                  ,0                  ,null               ,0           ,true),
(10      ,20     ,555500000000         ,105500000000           ,false                  ,0                  ,null               ,100000     ,true),
(20      ,30     ,100000000             ,100000000               ,false                  ,0                  ,null               ,104595     ,true ),
(30      ,40     ,250000000             ,200000000               ,false                  ,0                  ,null               ,104670     ,true ),
(40      ,50     ,15025000000000      ,14725000000000        ,false                  ,0                  ,null               ,105000     ,true ),
(50      ,60     ,25100000000000      ,22700000000000        ,false                  ,0                  ,null               ,106000     ,true ),
(60     ,70      ,77182383705332315  ,77182383705332315    ,false                  ,0                  ,null               ,141839     ,false),
(70     ,70      ,77216366305332315  ,77216366305332315    ,false                  ,0                  ,null               ,141844     ,false),
(80     ,70      ,77798522705332315  ,77798522705332315    ,false                  ,0                  ,null               ,141853     ,true),
(90     ,100     ,40767800000000      ,40767800000000        ,false                  ,0                  ,null               ,141855     ,false),
(100    ,100     ,41167700000000      ,41167700000000        ,false                  ,0                  ,10                 ,10000      ,true),
(110    ,110     ,2424711969422000   ,2424711969422000     ,false                  ,1150030000000  ,10                 ,10000      ,true),
(120    ,120     ,2424711869422000   ,2424711869422000     ,false                  ,1150030000000  ,20                 ,10000      ,true),
(130    ,130     ,2424711769422000   ,2424711769422000     ,false                  ,1150030000000  ,30                 ,10000      ,true),
(140    ,140     ,77200915499807515  ,77200915499807515    ,false                  ,0                  ,40                 ,10000      ,true),
(150    ,150     ,40367900000000      ,40367900000000        ,false                  ,0                  ,50                 ,10000      ,true)
;


INSERT INTO account_lease
(DB_ID, ID,     LESSOR_ID,  CURRENT_LEASING_HEIGHT_FROM, CURRENT_LEASING_HEIGHT_TO, CURRENT_LESSEE_ID, NEXT_LEASING_HEIGHT_FROM, NEXT_LEASING_HEIGHT_TO, NEXT_LESSEE_ID, HEIGHT, LATEST) VALUES
(1,     1000,         100,        7000,                        11000,                     10,                0,                       0,                      0,              5000,  true),
(2,     1100,         110,        7000,                        11000,                     10,                0,                       0,                      0,              5000,  true),
(3,     1200,         120,        7000,                        11000,                     20,                0,                       0,                      0,              5000,  true),
(4,     1300,         130,        8000,                        10000,                     30,                0,                       0,                      0,              8000,   true),
(5,     1400,         140,        8000,                        9000,                      40,                0,                       0,                      0,              7000,   false),
(6,     1400,         140,        9440,                        12440,                     50,                0,                       0,                      0,              8000,   true),
(7,     1500,         150,        9440,                        12440,                     50,                0,                       0,                      0,              8000,   true);
