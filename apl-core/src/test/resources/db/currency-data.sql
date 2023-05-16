delete from currency;

INSERT INTO currency
(DB_ID, ID,                 ACCOUNT_ID,             NAME,       NAME_LOWER, CODE, DESCRIPTION,                                            TYPE, INITIAL_SUPPLY, RESERVE_SUPPLY, MAX_SUPPLY,       CREATION_HEIGHT, ISSUANCE_HEIGHT, MIN_RESERVE_PER_UNIT_ATM, MIN_DIFFICULTY, MAX_DIFFICULTY, RULESET, ALGORITHM, DECIMALS, HEIGHT, LATEST, DELETED) VALUES
(1,     6847297283087791598, -6392448561240417498, 'Gold',      'gold',     'GLD', 'A new token allowing the easy trade of gold bullion.', 7,   9900000000000000, 0,            9900000000000000, 3015,             0,              0,                         0,               0,              0,      0,          5,      3015,   true,   false),
(2,     7766734660591439874, 2650055114867906720, 'APOLOCENK', 'apolocenk', 'APLCN','CENK',                                                7,   10000000,         20000000,     20000000,         98888,            98890,          100000,                    0,               0,              0,      0,          2,      98888,  true,   false),
(3,     2582192196243262007, 5122426243196961555, 'MarioCoin', 'mariocoin', 'SMB', 'Currency used in the Mushroom Kingdom, . 1UP',         1,   25000,            0,            25000,            104087,           0,              0,                         0,               0,              0,      0,          0,      104087, true,   false),
(4,     834406670971597912,  6869755601928778675, 'BitcoinX',  'bitcoinx',  'BTCX', 'Bitcoins distant relative Bitcoin X',                 1,   2100000000,       0,            2100000000,       104613,           0,              0,                         0,               0,              0,      0,          2,      104613, true,   false)
;
