DELETE
from currency_mint;

INSERT INTO currency_mint
(DB_ID, CURRENCY_ID   , ACCOUNT_ID  ,   COUNTER, HEIGHT  , LATEST , DELETED ) VALUES
(01,    1000,           100,            10,       800,      false , false),
(11,    1000,           100,            10,       820,      true,   false),
(20,    1000,           200,            20,       900,      false,  false),
(22,    1000,           200,            20,       920,      false,  true),
(23,    1000,           200,            20,       930,      true ,  false),
(30,    2000,           200,            30,       1000,     false,  false),
(33,    2000,           200,            30,       1003,     false,  false),
(34,    2000,           200,            30,       1004,     true,   false),
(40,    2000,           100,            40,       1100,     false,  false),
(41,    2000,           100,            40,       1101,     true,   false),
(50,    2000,           300,            50,       1200,     false,  false),
(59,    2000,           300,            50,       1209,     true,   false)
;