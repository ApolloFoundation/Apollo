delete from currency_supply;

INSERT INTO currency_supply
(DB_ID, ID,                 CURRENT_SUPPLY, CURRENT_RESERVE_PER_UNIT_ATM, HEIGHT, LATEST, DELETED) VALUES
(1,     6847297283087791598, 20000000,      100000,                       3015,      true, false),
(2,     7766734660591439874, 999000000,     10,                           98888,     true, false),
(3,     2582192196243262007, 50000,         200,                          104087,    true, false),
(4,     834406670971597912,  100000,        0,                            104613,    true, false)
;