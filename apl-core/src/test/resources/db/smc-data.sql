/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

/*
  It's IMPORTANT
       the sharp symbol is used as a statement delimiter for StringTokenizer in the DbPopulator class.
  Cause the smart contract code use the semicolon as statement delimiter.
*/

TRUNCATE TABLE smc_contract;
#
TRUNCATE TABLE smc_state;
#
TRUNCATE TABLE transaction;
#

INSERT INTO smc_contract (db_id, address, owner, transaction_id, transaction_full_hash, fuel_price, fuel_limit,
                          fuel_charged, block_timestamp, data, name, base_contract, args, language, version, status,
                          height, latest)
VALUES (2, 832074176060907552, -9064457886754183939, 832074176060907552,
        X'20E0E2FA0D1F8C0B35C5F415C406D9B6092DE3452AA65DCB780A6EE6C15DD962', 10000, 500000000, 42177663, 123723548,
        'class MyAPL20PerLock extends APL20PersonalLockable { constructor(){ super("NewSC","NewSC","100000000000000","100000000000","100000000","0","0x82349393da8764fd"); } }',
        'MyAPL20PersonalLockable', 'APL20PersonalLockable', '', 'js', '0.1.1', 'ACTIVE', 10, 1)
;
#

INSERT INTO smc_state (db_id, address, object, status, height, latest)
VALUES (875, 832074176060907552,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0x61a24f8dffd95e0efb346126ea75abad3c0735770b1484e898b70283b18112e6"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0xe9f31b7d60c1b7a479cc9c171c6fac88ebc73ed6fb040441033b75cbb4bf93d9"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x0081f7500e00"},"_name":"NewSC","_symbol":"NewSC","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x5af3107a4000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x05f5e100"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0x82349393da8764fd"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_initialSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x174876e800"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0xdeb56a6e1db35ee07ae053afe93207cb69f72b19beaa6b0ca9e15342a305555f"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0x686688b8389055ab9b3864631373f16b1c5af03160b64defdf9914daeab2c228"},"_releases":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0x3cb471f3fbb8dd118d8688aba0f5ac35a8b7508f0b1d9bdfdcb8c12ca0fbd0d1"},"_releaseDelay":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"meta":{"className":"MyAPL20PersonalLockable"}}',
        'ACTIVE', 10, 1)
;
#

INSERT INTO transaction
(db_id, id, deadline, recipient_id, transaction_index, amount, fee, full_hash, height, block_id, signature, TIMESTAMP,
 type, subtype, sender_id, sender_public_key, block_timestamp, referenced_transaction_full_hash, phased,
 attachment_bytes, version, has_message, has_encrypted_message, has_public_key_announcement, ec_block_height,
 ec_block_id, has_encrypttoself_message, has_prunable_message, has_prunable_encrypted_message, has_prunable_attachment)
VALUES (1, 832074176060907552, 1440, -6734560475180445013, 0, 0, 1931000000,
        X'20E0E2FA0D1F8C0B35C5F415C406D9B6092DE3452AA65DCB780A6EE6C15DD962', 10, 1938841353502506488,
        X'4D5349470000000001008F3F13CDBC4C2A8BC18F614D4A9419A2A925C6B6B9508D04360B5F44DA8BF793B486FF4F984A7705E45116196F8E571BC0E5A1B094EDF9BB649BBCDEBB7DD0EDD815A62F1AD90E7B',
        105502215, 11, 0, 9018499825772135441, X'8F3F13CDBC4C2A8B668BB8C0ABE09B668F851F10FA39A49535F777919086D618',
        105502204, NULL, 0,
        X'0180C3C901000000006400000000000000050000004465616C3390010000636C617373204465616C3320657874656E647320436F6E74726163747B0A636F6E7374727563746F72282076616C75652C2076656E646F7220297B0A737570657228293B0A746869732E76616C7565203D2076616C75653B0A746869732E76656E646F72203D2076656E646F723B0A746869732E637573746F6D6572203D2027273B0A746869732E70616964203D2066616C73653B0A746869732E6163636570746564203D2066616C73653B0A7D0A7061792829207B0A6966202820746869732E76616C7565203C3D206D73672E67657456616C7565282920297B0A746869732E70616964203D20747275653B0A746869732E637573746F6D6572203D206D73672E67657453656E64657228293B0A7D0A7D0A61636365707428297B0A6966202821746869732E616363657074656420262620746869732E70616964297B0A73757065722E73656E6428746869732E76616C75652C20534D432E6372656174654164647265737328746869732E76656E646F7229290A746869732E6163636570746564203D20747275650A7D0A7D0A7D1F000000323930303030303030302C27307864663134343263376335616433333932270A0000006A61766173637269707401FE55A9DA30B49B6D37D36AB6A6DD46797D4E2F66B3290F633FC19DF0F1CDC630',
        2, 0, 0, 1, 1832, -4000550348168638766, 0, 0, 0, 0)
;
#
