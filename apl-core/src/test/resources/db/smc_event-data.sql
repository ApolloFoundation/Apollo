/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */
TRUNCATE TABLE smc_event;
TRUNCATE TABLE smc_event_log;

INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (1, 8641622137343210570, 2715369145210035887, 1872215471858864549,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        49);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (2, -4669556940917485189, -5751338488940318740, 1872215471858864549,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 124);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (3, 958963192495881424, 8884117717076820141, 6167416064671373156,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        294);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (4, 8584388031921475893, 4690216519022424825, 6167416064671373156,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 304);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (5, -3220986732846876544, 6215280239318408678, 8194546512095429093,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        337);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (6, 3846775401837883461, 4985488184497144088, 8194546512095429093,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 359);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (7, 3121749937603690432, 4720017432190825177, 8194546512095429093,
        0x189E4D3E35F578C5C09F30685500DDAD3D9ED5A67EAFDC60CFFF5C9E75BAE72E, 'Freeze', 'Freeze:address,amount', 1, 0,
        514);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (8, -5857786578880170342, -2313115095631969295, 8194546512095429093,
        0xEB90B7B3DFB7E2CFD6DDC4C273614EC131CD19E037777B88C67975977B20CE1D, 'Unfreeze', 'Unfreeze:address,amount', 1, 0,
        520);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (9, 7295000778395662083, 4391502598325103093, 8194546512095429093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331, 'Approval', 'Approval:owner,spender,amount',
        2, 0, 651);
INSERT INTO smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (10, 1320833319573621558, 4321391335778711139, 8194546512095429093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331, 'Approval', 'Approval:owner,spender,amount',
        2, 0, 688);

INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (1, 8641622137343210570, 2715369145210035887, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x084595161401484a000000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x19fb720d5f2351a5"}}',
        0, 49);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (2, -4669556940917485189, -5751338488940318740,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x06a3a9e82800"},"who":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 124);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (3, -4669556940917485189, -4853775219044635418,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x00"},"who":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 286);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (4, 958963192495881424, 8884117717076820141, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x5af3107a4000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x5597103c04c65764"}}',
        0, 294);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (5, 8584388031921475893, 4690216519022424825, 0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x02540be400"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 304);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (6, -3220986732846876544, 6215280239318408678,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x5af3107a4000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x71b8e0a2dcdd41e5"}}',
        0, 337);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (7, 3846775401837883461, 4985488184497144088, 0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"who":{"class":"Address","value":"0x25dcf5c92f9e2c8b"}}',
        0, 359);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (8, -3220986732846876544, -7061934598452243102,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"from":{"class":"Address","value":"0x71b8e0a2dcdd41e5"},"to":{"class":"Address","value":"0x25dcf5c92f9e2c8b"}}',
        0, 364);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (9, -3220986732846876544, 482547755815197302, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x048c27395000"},"from":{"class":"Address","value":"0x25dcf5c92f9e2c8b"},"to":{"class":"Address","value":"0xcf6acfe4ab249827"}}',
        0, 407);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (10, 3121749937603690432, 4720017432190825177,
        0x189E4D3E35F578C5C09F30685500DDAD3D9ED5A67EAFDC60CFFF5C9E75BAE72E,
        '{"amount":"2000000000000","address":{"class":"Address","value":"0xcf6acfe4ab249827"}}', 0, 514);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (11, -5857786578880170342, -2313115095631969295,
        0xEB90B7B3DFB7E2CFD6DDC4C273614EC131CD19E037777B88C67975977B20CE1D,
        '{"amount":"2000000000000","address":{"class":"Address","value":"0xcf6acfe4ab249827"}}', 0, 520);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (12, 7295000778395662083, 4391502598325103093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331,
        '{"owner":{"class":"Address","value":"0xcf6acfe4ab249827"},"amount":{"class":"UnsignedBigNum","value":"0x01d1a94a2000"},"spender":{"class":"Address","value":"0x8686882ae20bc1c0"}}',
        0, 651);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (13, -3220986732846876544, 4321391335778711139,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x01d1a94a2000"},"from":{"class":"Address","value":"0xcf6acfe4ab249827"},"to":{"class":"Address","value":"0x2fb414b73dde9db4"}}',
        0, 688);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (14, 1320833319573621558, 4321391335778711139,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331,
        '{"owner":{"class":"Address","value":"0xcf6acfe4ab249827"},"amount":{"class":"UnsignedBigNum","value":"0x00"},"spender":{"class":"Address","value":"0x8686882ae20bc1c0"}}',
        0, 688);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (15, -3220986732846876544, -5037511862210495298,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x048c27395000"},"from":{"class":"Address","value":"0x25dcf5c92f9e2c8b"},"to":{"class":"Address","value":"0x00"}}',
        0, 709);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (16, -4669556940917485189, 4482300741931118413,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x00e8d4a51000"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8363);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (17, -4669556940917485189, -11988667360175078,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8375);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (18, 8641622137343210570, 4187599812016087260,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x0a012317b000"},"from":{"class":"Address","value":"0x19fb720d5f2351a5"},"to":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8399);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (19, 8641622137343210570, -681006609930740257,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x02540be400"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8406);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (20, 8641622137343210570, -1044419293482076321,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8414);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (21, 8641622137343210570, 5120829338053502180,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8459);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (22, 8641622137343210570, -1074906282696789803,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8472);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (23, 8641622137343210570, -5865217747736841704,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8711);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (24, 8641622137343210570, 876428265968044594, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8735);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (25, 8641622137343210570, 1522295713134715419,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8792);
INSERT INTO smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (26, 8641622137343210570, -5555587303535125302,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 13085);
