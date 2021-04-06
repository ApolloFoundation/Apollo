TRUNCATE TABLE smc_contract;
TRUNCATE TABLE smc_state;

INSERT INTO smc_contract
(`db_id`, `address`, `owner`, `transaction_id`, `data`, `name`, `args`, `language`, `version`, `status`, `height`)
values (1, 7307657537262705518, 3705364957971254799, -3674815124405720630, 'class Deal extends Contract{}', 'Deal',
        '1400000000,"APL-X5JH-TJKJ-DVGC-5T2V8"', 'javascript', '0.1.1', 'ACTIVE', 10)
;

INSERT INTO smc_state
    (db_id, address, object, status, height, latest)
VALUES (1, 7307657537262705518,
        '{"value":1400000000,"vendor":"APL-X5JH-TJKJ-DVGC-5T2V8","customer":"","paid":false,"accepted":false}',
        'ACTIVE', 10, TRUE)
;
