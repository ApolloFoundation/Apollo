/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

/*
  It's IMPORTANT
       the sharp symbol is used as a statement delimiter for StringTokenizer in the DbPopulator class.
  Cause the smart contract code use the semicolon as statement delimiter.
*/

TRUNCATE TABLE smc_contract;#
TRUNCATE TABLE smc_state;#
TRUNCATE TABLE smc_mapping;#
TRUNCATE TABLE smc_event;#
TRUNCATE TABLE smc_event_log;#

insert into smc_state (db_id, address, object, status, height, latest, deleted)
values (5, 6167416064671373156,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"de672a61-7098-415d-8458-e8deb3f88e3b"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"930cae1d-aba2-4f11-8bc2-a0fd06841858"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x5af3107a4000"},"_name":"Deal2","_symbol":"ntk","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x5af3107a4000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x3b9aca00"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0x54cb3319380bdb3a"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"f73e63f2-b6bf-4522-8bba-f4a7aede736b"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"40a0e3d9-516b-4cbb-b300-def7a799ceb6"},"_releaseTime":1.632410520167E12,"meta":{"className":"MyAPL20LOCK"}}',
        'ACTIVE', 304, 1, 0),
       (14, 8194546512095429093,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"63aae226-3bd1-4a46-879a-0f7ecc87f22b"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"a1c22622-6f41-4986-8a13-880640575e6c"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x5666e940f000"},"_name":"SC TEST","_symbol":"SC TEST","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x5af3107a4000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x3b9aca00"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0xd93e7865b61ad38a"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"0898ce8f-3763-4610-9f30-ca09caa3ef41"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"249841e5-7ab3-4db2-baeb-de14105c00ab"},"_releaseTime":1.632389460413E12,"meta":{"className":"MyAPL20LOCK"}}',
        'ACTIVE', 709, 1, 0),
       (24, 1872215471858864549,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"82118a98-7999-4159-ba8a-680dee8d7d91"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"b43c68de-7fb1-4a54-9708-d0da53929256"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_name":"SimpleFB_Token","_symbol":"FBT","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x02540be400"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0x7d2825cf3a120411"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"bbac5919-1f81-41fc-af1c-b421cfe7cf2b"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"3d8fe2f9-e147-4199-b30d-84d258204800"},"_releaseTime":1.631870400293E12,"meta":{"className":"MyAPL20LOCK"}}',
        'ACTIVE', 8792, 0, 0),
       (25, 1872215471858864549,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"82118a98-7999-4159-ba8a-680dee8d7d91"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"b43c68de-7fb1-4a54-9708-d0da53929256"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_name":"SimpleFB_Token","_symbol":"FBT","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x02540be400"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0x7d2825cf3a120411"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"bbac5919-1f81-41fc-af1c-b421cfe7cf2b"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"3d8fe2f9-e147-4199-b30d-84d258204800"},"_releaseTime":1.631870400293E12,"meta":{"className":"MyAPL20LOCK"}}',
        'ACTIVE', 13085, 0, 0),
       (26, 1872215471858864549,
        '{"_balances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"82118a98-7999-4159-ba8a-680dee8d7d91"},"_allowances":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"b43c68de-7fb1-4a54-9708-d0da53929256"},"_totalSupply":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_name":"SimpleFB_Token","_symbol":"FBT","_Transfer":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Transfer:from,to,amount","indexedFieldsCount":2,"anonymous":false},"_Approval":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Approval:owner,spender,amount","indexedFieldsCount":2,"anonymous":false},"_cap":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x084595161401484a000000"},"_rate":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x02540be400"},"_vault":{"meta":{"className":"Address","args":["value"],"create":"DSH.createAddress"},"value":"0x7d2825cf3a120411"},"_totalBuy":{"meta":{"className":"UnsignedBigNum","args":["value"],"create":"DSH.createUnsignedBigNum"},"value":"0x00"},"_Buy":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Buy:who,amount","indexedFieldsCount":1,"anonymous":false},"_freezes":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"bbac5919-1f81-41fc-af1c-b421cfe7cf2b"},"_Freeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Freeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_Unfreeze":{"meta":{"className":"Event","args":["spec","indexedFieldsCount","anonymous"],"create":"DSH.createEventType"},"spec":"Unfreeze:address,amount","indexedFieldsCount":1,"anonymous":false},"_locks":{"meta":{"className":"UnsignedBigNumMappingRepository","args":["value"],"create":"DSH.unsignedBigNumMapping"},"value":"3d8fe2f9-e147-4199-b30d-84d258204800"},"_releaseTime":1.631870400293E12,"meta":{"className":"MyAPL20LOCK"}}',
        'ACTIVE', 13097, 1, 0)
;#
insert into smc_mapping (db_id, address, entry_key, name, object, height, latest, deleted)
values (3, 1872215471858864549, 0x957EE43200D32B1FBD89B4922AA98876F1B519F6DA60B4FE99258C147FCA0782,
        '3d8fe2f9-e147-4199-b30d-84d258204800', '0x06a3a9e82800', 286, 1, 0),
       (4, 6167416064671373156, 0x167F09CEB37B9F97A0A62833BA66919F9F341950762CC0CCD09D33C46530F3F3,
        'de672a61-7098-415d-8458-e8deb3f88e3b', '0x5af3107a4000', 294, 1, 0),
       (5, 6167416064671373156, 0x0D20508A235431308BFA60854B78CA9738C8104C6D9F6A834578613E12A56F35,
        '40a0e3d9-516b-4cbb-b300-def7a799ceb6', '0x02540be400', 304, 1, 0),
       (8, 8194546512095429093, 0x41A7F2AB62D1CB441D2B4544D6BE315D85D5A57FDB369E866C98FBF8A5AC0818,
        '249841e5-7ab3-4db2-baeb-de14105c00ab', '0x00', 364, 1, 0),
       (9, 8194546512095429093, 0xB136C854D28176975F851B9184FC3C301A29994E08BF9475413A6B1D733D67C2,
        '63aae226-3bd1-4a46-879a-0f7ecc87f22b', '0x51dac207a000', 364, 1, 0),
       (15, 8194546512095429093, 0xD7EAFB38B98F057B2EE473C7DDD880BF786AB3B5EF1FA16128F48525C24BD44B,
        '0898ce8f-3763-4610-9f30-ca09caa3ef41', '0x00', 520, 1, 0),
       (18, 8194546512095429093, 0x57375ADBE2CF0181097DAD27F7A892144829379583CD103E59C0EA44F76461E8,
        'a1c22622-6f41-4986-8a13-880640575e6c', '0x00', 688, 1, 0),
       (19, 8194546512095429093, 0xD7EAFB38B98F057B2EE473C7DDD880BF786AB3B5EF1FA16128F48525C24BD44B,
        '63aae226-3bd1-4a46-879a-0f7ecc87f22b', '0x02ba7def3000', 688, 1, 0),
       (20, 8194546512095429093, 0xF93FA26A8FA5E339AE1E0D3DA9B43D362031C31009C99207B4941A945E038167,
        '63aae226-3bd1-4a46-879a-0f7ecc87f22b', '0x01d1a94a2000', 688, 1, 0),
       (21, 8194546512095429093, 0x41A7F2AB62D1CB441D2B4544D6BE315D85D5A57FDB369E866C98FBF8A5AC0818,
        '63aae226-3bd1-4a46-879a-0f7ecc87f22b', '0x00', 709, 1, 0),
       (25, 1872215471858864549, 0xBAD70FE5C5A3E350A6772ADDC2DE612C4AB586CC853331CF3DB35D35D7BEB3D3,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x0845951613f74726e85000', 8399, 1, 0),
       (26, 1872215471858864549, 0x31A28A0E46815291FDD79387C8E5FE633B3083722544C0AA080C6E3BE47C018E,
        '3d8fe2f9-e147-4199-b30d-84d258204800', '0x00', 8399, 1, 0),
       (39, 1872215471858864549, 0x31A28A0E46815291FDD79387C8E5FE633B3083722544C0AA080C6E3BE47C018E,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x09fd696b1000', 8792, 0, 0),
       (40, 1872215471858864549, 0x957EE43200D32B1FBD89B4922AA98876F1B519F6DA60B4FE99258C147FCA0782,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x03b9aca000', 8792, 0, 0),
       (41, 1872215471858864549, 0x31A28A0E46815291FDD79387C8E5FE633B3083722544C0AA080C6E3BE47C018E,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x09fd2dd04600', 13085, 0, 0),
       (42, 1872215471858864549, 0x957EE43200D32B1FBD89B4922AA98876F1B519F6DA60B4FE99258C147FCA0782,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x03f5476a00', 13085, 0, 0),
       (43, 1872215471858864549, 0x31A28A0E46815291FDD79387C8E5FE633B3083722544C0AA080C6E3BE47C018E,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x09fcf2357c00', 13097, 1, 0),
       (44, 1872215471858864549, 0x957EE43200D32B1FBD89B4922AA98876F1B519F6DA60B4FE99258C147FCA0782,
        '82118a98-7999-4159-ba8a-680dee8d7d91', '0x0430e23400', 13097, 1, 0)
;#
insert into smc_event (db_id, id, transaction_id, contract, signature, name, spec, idx_count, is_anonymous, height)
VALUES (1, 8641622137343210570, 2715369145210035887, 1872215471858864549,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        49),
       (2, -4669556940917485189, -5751338488940318740, 1872215471858864549,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 124),
       (3, 958963192495881424, 8884117717076820141, 6167416064671373156,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        294),
       (4, 8584388031921475893, 4690216519022424825, 6167416064671373156,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 304),
       (5, -3220986732846876544, 6215280239318408678, 8194546512095429093,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D, 'Transfer', 'Transfer:from,to,amount', 2, 0,
        337),
       (6, 3846775401837883461, 4985488184497144088, 8194546512095429093,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967, 'Buy', 'Buy:who,amount', 1, 0, 359),
       (7, 3121749937603690432, 4720017432190825177, 8194546512095429093,
        0x189E4D3E35F578C5C09F30685500DDAD3D9ED5A67EAFDC60CFFF5C9E75BAE72E, 'Freeze', 'Freeze:address,amount', 1, 0,
        514),
       (8, -5857786578880170342, -2313115095631969295, 8194546512095429093,
        0xEB90B7B3DFB7E2CFD6DDC4C273614EC131CD19E037777B88C67975977B20CE1D, 'Unfreeze', 'Unfreeze:address,amount', 1, 0,
        520),
       (9, 7295000778395662083, 4391502598325103093, 8194546512095429093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331, 'Approval', 'Approval:owner,spender,amount',
        2, 0, 651),
       (10, 1320833319573621558, 4321391335778711139, 8194546512095429093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331, 'Approval', 'Approval:owner,spender,amount',
        2, 0, 688)
;#
insert into smc_event_log (db_id, event_id, transaction_id, signature, state, tx_idx, height)
VALUES (1, 8641622137343210570, 2715369145210035887, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x084595161401484a000000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x19fb720d5f2351a5"}}',
        0, 49),
       (2, -4669556940917485189, -5751338488940318740,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x06a3a9e82800"},"who":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 124),
       (3, -4669556940917485189, -4853775219044635418,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x00"},"who":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 286),
       (4, 958963192495881424, 8884117717076820141, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x5af3107a4000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x5597103c04c65764"}}',
        0, 294),
       (5, 8584388031921475893, 4690216519022424825, 0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x02540be400"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 304),
       (6, -3220986732846876544, 6215280239318408678,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x5af3107a4000"},"from":{"class":"Address","value":"0x00"},"to":{"class":"Address","value":"0x71b8e0a2dcdd41e5"}}',
        0, 337),
       (7, 3846775401837883461, 4985488184497144088, 0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"who":{"class":"Address","value":"0x25dcf5c92f9e2c8b"}}',
        0, 359),
       (8, -3220986732846876544, -7061934598452243102,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"from":{"class":"Address","value":"0x71b8e0a2dcdd41e5"},"to":{"class":"Address","value":"0x25dcf5c92f9e2c8b"}}',
        0, 364),
       (9, -3220986732846876544, 482547755815197302, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x048c27395000"},"from":{"class":"Address","value":"0x25dcf5c92f9e2c8b"},"to":{"class":"Address","value":"0xcf6acfe4ab249827"}}',
        0, 407),
       (10, 3121749937603690432, 4720017432190825177,
        0x189E4D3E35F578C5C09F30685500DDAD3D9ED5A67EAFDC60CFFF5C9E75BAE72E,
        '{"amount":"2000000000000","address":{"class":"Address","value":"0xcf6acfe4ab249827"}}', 0, 514),
       (11, -5857786578880170342, -2313115095631969295,
        0xEB90B7B3DFB7E2CFD6DDC4C273614EC131CD19E037777B88C67975977B20CE1D,
        '{"amount":"2000000000000","address":{"class":"Address","value":"0xcf6acfe4ab249827"}}', 0, 520),
       (12, 7295000778395662083, 4391502598325103093,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331,
        '{"owner":{"class":"Address","value":"0xcf6acfe4ab249827"},"amount":{"class":"UnsignedBigNum","value":"0x01d1a94a2000"},"spender":{"class":"Address","value":"0x8686882ae20bc1c0"}}',
        0, 651),
       (13, -3220986732846876544, 4321391335778711139,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x01d1a94a2000"},"from":{"class":"Address","value":"0xcf6acfe4ab249827"},"to":{"class":"Address","value":"0x2fb414b73dde9db4"}}',
        0, 688),
       (14, 1320833319573621558, 4321391335778711139,
        0x9E2F1EEE77A3FAEBCA4A9270A2E5C3F5E5C9A42F565F205FD39DF01348861331,
        '{"owner":{"class":"Address","value":"0xcf6acfe4ab249827"},"amount":{"class":"UnsignedBigNum","value":"0x00"},"spender":{"class":"Address","value":"0x8686882ae20bc1c0"}}',
        0, 688),
       (15, -3220986732846876544, -5037511862210495298,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x048c27395000"},"from":{"class":"Address","value":"0x25dcf5c92f9e2c8b"},"to":{"class":"Address","value":"0x00"}}',
        0, 709),
       (16, -4669556940917485189, 4482300741931118413,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x00e8d4a51000"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8363),
       (17, -4669556940917485189, -11988667360175078,
        0x043339AD5FA736E08C5DBB5C5B5074D75D76E612DACE879CAC1CCF93E9DCC967,
        '{"amount":{"class":"UnsignedBigNum","value":"0x09184e72a000"},"who":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8375),
       (18, 8641622137343210570, 4187599812016087260,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x0a012317b000"},"from":{"class":"Address","value":"0x19fb720d5f2351a5"},"to":{"class":"Address","value":"0x42f7aa92f9586e8b"}}',
        0, 8399),
       (19, 8641622137343210570, -681006609930740257,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x02540be400"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8406),
       (20, 8641622137343210570, -1044419293482076321,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8414),
       (21, 8641622137343210570, 5120829338053502180,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8459),
       (22, 8641622137343210570, -1074906282696789803,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8472),
       (23, 8641622137343210570, -5865217747736841704,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8711),
       (24, 8641622137343210570, 876428265968044594, 0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8735),
       (25, 8641622137343210570, 1522295713134715419,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 8792),
       (26, 8641622137343210570, -5555587303535125302,
        0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D,
        '{"amount":{"class":"UnsignedBigNum","value":"0x3b9aca00"},"from":{"class":"Address","value":"0x42f7aa92f9586e8b"},"to":{"class":"Address","value":"0x336c19cc230e8e0f"}}',
        0, 13085)
;#
insert into smc_contract (db_id, address, owner, transaction_id, data, name, base_contract, args, language, version,
                          status, height, latest)
VALUES (1, 1872215471858864549, 3705364957971254799, 2715369145210035887, '
class Contract {
emit(event, ...params) {
if (isTraceEnabled()) {
Contract.discoveryObject(event, false);
}
require(typeof event !== "undefined", "Missing event type.")
require(Java.isJavaObject(event), "Wrong event type.")
EventManager.emit(event, params);
}
static deserialize(dataString) {
let dataObject = JSON.parse(dataString);
if (dataObject && dataObject.meta) {
if (dataObject.meta.create) {
try {
const values = dataObject.meta.args?.map(arg => dataObject[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className} creator=${dataObject.meta.create} args=${values}`);
}
dataObject = eval(dataObject.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + dataObject.meta.create);
throw e;
}
} else if (dataObject.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(dataObject.meta.className);
} catch (e) {
console.log(''Class not found: '' + dataObject.meta.className);
throw e;
}
if (originalClass && originalClass.prototype) {
const oldMainClassObjProto = originalClass.prototype;
const deepParse = function (classObj, partDataObject) {
if (classObj && classObj.prototype) {
const oldProto = classObj.prototype;
const classNew = function () {
for (const field in partDataObject) {
let fieldData = partDataObject[field];
if (fieldData && typeof fieldData === ''object'' && ''meta'' in fieldData) {
if (fieldData.meta.create) {
try {
const values = fieldData.meta.args?.map(arg => fieldData[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className} creator=${fieldData.meta.create} args=${values}`);
}
this[field] = eval(fieldData.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + fieldData.meta.create);
throw e;
}
} else if (fieldData.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(fieldData.meta.className);
} catch (e) {
console.log(''Class not found: '' + fieldData.meta.className);
throw e;
}
this[field] = new deepParse(originalClass, fieldData);
}
} else {
this[field] = fieldData;
}
}
};
classNew.prototype = oldProto;
return new classNew();
} else {
return partDataObject;
}
};
deepParse.prototype = oldMainClassObjProto;
return new deepParse(originalClass, dataObject);
} else {
return dataObject;
}
}
} else {
return dataObject;
}
}
static discoveryObject(object, elements) {
console.log(`***Got object=${object} type=${typeof object} isJavaObject=${Java.isJavaObject(object)}`)
if (typeof object === "object" && elements) {
for (const element in object) {
console.log(`*** element=${element} value=${object[element]}`)
}
}
}
}
class APL20 extends Contract {
_balances = Mapping.UBN();
_allowances = Mapping.UBN();
_totalSupply = new UBN();
_name = '''';
_symbol = '''';
_Transfer = new Event("Transfer:from,to,amount", 2);
_Approval = new Event("Approval:owner,spender,amount", 2);
constructor(name, symbol) {
super();
require(name !== undefined && symbol !== undefined, "APL20: constructor undefined parameter");
this._name = name;
this._symbol = symbol;
}
name() {
return this._name;
}
symbol() {
return this._symbol;
}
decimals() {
return new UBN(''8'');
}
totalSupply() {
return this._totalSupply;
}
balanceOf(account) {
const addr = address(account);
return this._balances.get(addr);
}
transfer(recipient, amount) {
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(msg.sender(), recipientAddr, value);
return true;
}
allowance(owner, spender) {
const ownerAddr = address(owner);
const spenderAddr = address(spender);
return this._allowances.get(ownerAddr, spenderAddr);
}
approve(spender, amount) {
const spenderAddr = address(spender);
const value = new UBN(amount);
this._approve(msg.sender(), spenderAddr, value);
return true;
}
transferFrom(sender, recipient, amount) {
const senderAddr = address(sender);
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(senderAddr, recipientAddr, value);
this._approve(senderAddr, msg.sender(), this._allowances.get(senderAddr, msg.sender()).sub(value));
return true;
}
increaseAllowance(spender, addedValue) {
const spenderAddr = address(spender);
const value = new UBN(addedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).add(value));
return true;
}
decreaseAllowance(spender, subtractedValue) {
const spenderAddr = address(spender);
const value = new UBN(subtractedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).sub(value));
return true;
}
_transfer(senderAddr, recipientAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===TRANSFER: sender=${senderAddr}, recipient=${recipientAddr}, amount=${amountValue}`);
}
const sender = address(senderAddr);
const recipient = address(recipientAddr);
const amount = new UBN(amountValue);
require(sender && sender !== address(''0x0''), "APL20: transfer from the zero address");
require(recipient && recipient !== address(''0x0''), "APL20: transfer to the zero address");
if (isTraceEnabled()) {
console.log(`===TRANSFER: 1: sender balance=${this._balances.get(sender)}`);
}
require(this._balances.get(sender).gte(amount), "APL20: transfer amount exceeds balance");
this._balances.set(sender, this._balances.get(sender).sub(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: sender balance=${this._balances.get(sender)}`);
console.log(`===TRANSFER: 1: recipient balance=${this._balances.get(recipient)}`);
}
this._balances.set(recipient, this._balances.get(recipient).add(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: recipient balance=${this._balances.get(recipient)}`);
}
this.emit(this._Transfer, sender, recipient, amount);
}
_mint(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===MINT: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== address(''0x0''), "APL20: mint to the zero address");
this._totalSupply = this._totalSupply.add(amount);
if (isTraceEnabled()) {
console.log(`===MINT: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).add(amount));
if (isTraceEnabled()) {
console.log(`===MINT: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, ADDRESS_ZERO, account, amount);
}
_burn(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===BURN: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== ADDRESS_ZERO, "APL20: burn from the zero address");
this._totalSupply = this._totalSupply.sub(amount);
if (isTraceEnabled()) {
console.log(`===BURN: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).sub(amount));
if (isTraceEnabled()) {
console.log(`===BURN: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, account, ADDRESS_ZERO, amount);
}
_approve(ownerAddr, spenderAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===APROVE: owner=${ownerAddr}, spender=${spenderAddr}, value=${amountValue}, sender=${msg.sender()}`);
}
const spender = address(spenderAddr);
const owner = address(ownerAddr);
const amount = new UBN(amountValue);
require(owner !== ADDRESS_ZERO, "APL20: approve from the zero address");
require(spender !== ADDRESS_ZERO, "APL20: approve to the zero address");
this._allowances.set(owner, spender, amount);
this.emit(this._Approval, owner, spender, amount)
}
}
class APL20CAP extends APL20 {
_cap = new UBN();
constructor(name, symbol, cap) {
super(name, symbol);
const capValue = new UBN(cap)
require(capValue.gt(UBN.ZERO), "APL20CAP: cap is 0");
this._cap = capValue;
}
cap() {
return this._cap;
}
_mint(account, amount) {
const accountAddr = address(account);
const amountValue = new UBN(amount);
require(this.totalSupply().add(amountValue).lte(this._cap), "APL20CAP: cap exceeded");
super._mint(accountAddr, amountValue);
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| cap: ${fromAtm(this.cap())}`);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BURN extends APL20CAP {
burn(amount) {
this._burn(msg.sender(), new UBN(amount));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BUY extends APL20BURN {
_rate = new UBN(1);
_vault = address(''0x0'');
_totalBuy = new UBN();
_Buy = new Event("Buy:who,amount", 1);
constructor(name, symbol, cap, initialSupply, rate, vault) {
super(name, symbol, cap);
this._rate = new UBN(rate);
this._vault = address(vault);
super._mint(address(self), new UBN(initialSupply));
}
vault() {
return this._vault;
}
rate() {
return this._rate;
}
buy() {
let amount = msg.value().mul(this._rate).div(APL);
this._totalBuy.add(amount);
require(this._totalBuy.lte(this._totalSupply), "APL20BUY: exceeded the total buy amount.");
address(this._vault).transfer(msg.value());
this.emit(this._Buy, msg.sender(), amount)
return amount;
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per apl)`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log(`| total buy: ${this._totalBuy} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20FREEZE extends APL20BUY {
_freezes = Mapping.UBN();
_Freeze = new Event(''Freeze:address,amount'', 1);
_Unfreeze = new Event(''Unfreeze:address,amount'', 1);
freeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._balances.set(account, this._balances.get(account).sub(amountValue));
this._freezes.set(account, this._freezes.get(account).add(amountValue));
this.emit(this._Freeze, msg.sender(), amount);
}
unfreeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._freezes.set(account, this._freezes.get(account).sub(amountValue));
this._balances.set(account, this._balances.get(account).add(amountValue));
this.emit(this._Unfreeze, msg.sender(), amount);
}
freezeOf(account) {
return this._freezes.get(address(account));
}
}
class APL20LOCK extends APL20FREEZE {
_locks = Mapping.UBN();
_releaseTime = 0;
constructor(name, symbol, cap, initialSupply, rate, releaseTime, vault) {
super(name, symbol, cap, initialSupply, rate, vault);
if (isTraceEnabled()) {
console.log(`APL20LOCK: arguments releaseTime=${releaseTime}, initialSupply=${initialSupply}`);
}
const isoReleaseTime = toTimestamp(releaseTime);
if (isTraceEnabled()) {
console.log(`APL20LOCK: _releaseTime=${isoReleaseTime}, block.timeStamp=${block.timestamp()}`);
}
require(isoReleaseTime / 1000 > block.timestamp(), "APL20Lockable: release time is before current block time");
this._releaseTime = isoReleaseTime;
}
releaseTime() {
return this._releaseTime;
}
buy() {
const amount = super.buy();
const account = msg.sender();
this._locks.set(account, this._locks.get(account).add(amount));
return amount;
}
unlock() {
const account = msg.sender();
const amount = this.lockOf(account);
if (isTraceEnabled()) {
console.log(`APL20LOCK: unlock releaseTime=${this._releaseTime} block.timestamp()=${block.timestamp()}`);
}
require(block.timestamp() >= this.releaseTime() / 1000, "APL20Lockable: current time is before release time");
this._transfer(address(self), account, amount);
this._locks.set(account, UBN.ZERO);
return true;
}
lockOf(account) {
return this._locks.get(address(account));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per APL)`);
console.log(`| cap: ${fromAtm(this.cap())} `);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log(`| vault: ${this.vault().getHex()}`);
console.log(`| r. timestamp: ${this.releaseTime() / 1000} in seconds`);
console.log(`| release time: ${new Date(this.releaseTime()).toISOString()}`);
console.log("|---------------------------------------------------------------");
console.log(`|blk.timestamp: ${new Date(block.timestamp() * 1000).toISOString()}`);
console.log(`| current time: ${new Date().toISOString()}`);
console.log("|---------------------------------------------------------------\\n");
}
}
class MyAPL20LOCK extends APL20LOCK {
constructor(){
super(''SimpleFB_Token'',''FBT'',''10000000000000000000000000'',''10000000000000000000000000'',''10000000000'',''2021-09-17T09:20:00.293Z'',''0x7d2825cf3a120411'');
}
}', 'MyAPL20LOCK', 'APL20LOCK', '', 'js', '0.1.1', 'ACTIVE', 49, 1)
;#
insert into smc_contract (db_id, address, owner, transaction_id, data, name, base_contract, args, language, version,
                          status, height, latest)
VALUES (2, 6167416064671373156, 4825513073976045195, 8884117717076820141, '
class Contract {
emit(event, ...params) {
if (isTraceEnabled()) {
Contract.discoveryObject(event, false);
}
require(typeof event !== "undefined", "Missing event type.")
require(Java.isJavaObject(event), "Wrong event type.")
EventManager.emit(event, params);
}
static deserialize(dataString) {
let dataObject = JSON.parse(dataString);
if (dataObject && dataObject.meta) {
if (dataObject.meta.create) {
try {
const values = dataObject.meta.args?.map(arg => dataObject[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className} creator=${dataObject.meta.create} args=${values}`);
}
dataObject = eval(dataObject.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + dataObject.meta.create);
throw e;
}
} else if (dataObject.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(dataObject.meta.className);
} catch (e) {
console.log(''Class not found: '' + dataObject.meta.className);
throw e;
}
if (originalClass && originalClass.prototype) {
const oldMainClassObjProto = originalClass.prototype;
const deepParse = function (classObj, partDataObject) {
if (classObj && classObj.prototype) {
const oldProto = classObj.prototype;
const classNew = function () {
for (const field in partDataObject) {
let fieldData = partDataObject[field];
if (fieldData && typeof fieldData === ''object'' && ''meta'' in fieldData) {
if (fieldData.meta.create) {
try {
const values = fieldData.meta.args?.map(arg => fieldData[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className} creator=${fieldData.meta.create} args=${values}`);
}
this[field] = eval(fieldData.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + fieldData.meta.create);
throw e;
}
} else if (fieldData.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(fieldData.meta.className);
} catch (e) {
console.log(''Class not found: '' + fieldData.meta.className);
throw e;
}
this[field] = new deepParse(originalClass, fieldData);
}
} else {
this[field] = fieldData;
}
}
};
classNew.prototype = oldProto;
return new classNew();
} else {
return partDataObject;
}
};
deepParse.prototype = oldMainClassObjProto;
return new deepParse(originalClass, dataObject);
} else {
return dataObject;
}
}
} else {
return dataObject;
}
}
static discoveryObject(object, elements) {
console.log(`***Got object=${object} type=${typeof object} isJavaObject=${Java.isJavaObject(object)}`)
if (typeof object === "object" && elements) {
for (const element in object) {
console.log(`*** element=${element} value=${object[element]}`)
}
}
}
}
class APL20 extends Contract {
_balances = Mapping.UBN();
_allowances = Mapping.UBN();
_totalSupply = new UBN();
_name = '''';
_symbol = '''';
_Transfer = new Event("Transfer:from,to,amount", 2);
_Approval = new Event("Approval:owner,spender,amount", 2);
constructor(name, symbol) {
super();
require(name !== undefined && symbol !== undefined, "APL20: constructor undefined parameter");
this._name = name;
this._symbol = symbol;
}
name() {
return this._name;
}
symbol() {
return this._symbol;
}
decimals() {
return new UBN(''8'');
}
totalSupply() {
return this._totalSupply;
}
balanceOf(account) {
const addr = address(account);
return this._balances.get(addr);
}
transfer(recipient, amount) {
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(msg.sender(), recipientAddr, value);
return true;
}
allowance(owner, spender) {
const ownerAddr = address(owner);
const spenderAddr = address(spender);
return this._allowances.get(ownerAddr, spenderAddr);
}
approve(spender, amount) {
const spenderAddr = address(spender);
const value = new UBN(amount);
this._approve(msg.sender(), spenderAddr, value);
return true;
}
transferFrom(sender, recipient, amount) {
const senderAddr = address(sender);
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(senderAddr, recipientAddr, value);
this._approve(senderAddr, msg.sender(), this._allowances.get(senderAddr, msg.sender()).sub(value));
return true;
}
increaseAllowance(spender, addedValue) {
const spenderAddr = address(spender);
const value = new UBN(addedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).add(value));
return true;
}
decreaseAllowance(spender, subtractedValue) {
const spenderAddr = address(spender);
const value = new UBN(subtractedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).sub(value));
return true;
}
_transfer(senderAddr, recipientAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===TRANSFER: sender=${senderAddr}, recipient=${recipientAddr}, amount=${amountValue}`);
}
const sender = address(senderAddr);
const recipient = address(recipientAddr);
const amount = new UBN(amountValue);
require(sender && sender !== address(''0x0''), "APL20: transfer from the zero address");
require(recipient && recipient !== address(''0x0''), "APL20: transfer to the zero address");
if (isTraceEnabled()) {
console.log(`===TRANSFER: 1: sender balance=${this._balances.get(sender)}`);
}
require(this._balances.get(sender).gte(amount), "APL20: transfer amount exceeds balance");
this._balances.set(sender, this._balances.get(sender).sub(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: sender balance=${this._balances.get(sender)}`);
console.log(`===TRANSFER: 1: recipient balance=${this._balances.get(recipient)}`);
}
this._balances.set(recipient, this._balances.get(recipient).add(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: recipient balance=${this._balances.get(recipient)}`);
}
this.emit(this._Transfer, sender, recipient, amount);
}
_mint(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===MINT: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== address(''0x0''), "APL20: mint to the zero address");
this._totalSupply = this._totalSupply.add(amount);
if (isTraceEnabled()) {
console.log(`===MINT: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).add(amount));
if (isTraceEnabled()) {
console.log(`===MINT: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, ADDRESS_ZERO, account, amount);
}
_burn(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===BURN: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== ADDRESS_ZERO, "APL20: burn from the zero address");
this._totalSupply = this._totalSupply.sub(amount);
if (isTraceEnabled()) {
console.log(`===BURN: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).sub(amount));
if (isTraceEnabled()) {
console.log(`===BURN: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, account, ADDRESS_ZERO, amount);
}
_approve(ownerAddr, spenderAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===APROVE: owner=${ownerAddr}, spender=${spenderAddr}, value=${amountValue}, sender=${msg.sender()}`);
}
const spender = address(spenderAddr);
const owner = address(ownerAddr);
const amount = new UBN(amountValue);
require(owner !== ADDRESS_ZERO, "APL20: approve from the zero address");
require(spender !== ADDRESS_ZERO, "APL20: approve to the zero address");
this._allowances.set(owner, spender, amount);
this.emit(this._Approval, owner, spender, amount)
}
}
class APL20CAP extends APL20 {
_cap = new UBN();
constructor(name, symbol, cap) {
super(name, symbol);
const capValue = new UBN(cap)
require(capValue.gt(UBN.ZERO), "APL20CAP: cap is 0");
this._cap = capValue;
}
cap() {
return this._cap;
}
_mint(account, amount) {
const accountAddr = address(account);
const amountValue = new UBN(amount);
require(this.totalSupply().add(amountValue).lte(this._cap), "APL20CAP: cap exceeded");
super._mint(accountAddr, amountValue);
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| cap: ${fromAtm(this.cap())}`);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BURN extends APL20CAP {
burn(amount) {
this._burn(msg.sender(), new UBN(amount));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BUY extends APL20BURN {
_rate = new UBN(1);
_vault = address(''0x0'');
_totalBuy = new UBN();
_Buy = new Event("Buy:who,amount", 1);
constructor(name, symbol, cap, initialSupply, rate, vault) {
super(name, symbol, cap);
this._rate = new UBN(rate);
this._vault = address(vault);
super._mint(address(self), new UBN(initialSupply));
}
vault() {
return this._vault;
}
rate() {
return this._rate;
}
buy() {
let amount = msg.value().mul(this._rate).div(APL);
this._totalBuy.add(amount);
require(this._totalBuy.lte(this._totalSupply), "APL20BUY: exceeded the total buy amount.");
address(this._vault).transfer(msg.value());
this.emit(this._Buy, msg.sender(), amount)
return amount;
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per apl)`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log(`| total buy: ${this._totalBuy} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20FREEZE extends APL20BUY {
_freezes = Mapping.UBN();
_Freeze = new Event(''Freeze:address,amount'', 1);
_Unfreeze = new Event(''Unfreeze:address,amount'', 1);
freeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._balances.set(account, this._balances.get(account).sub(amountValue));
this._freezes.set(account, this._freezes.get(account).add(amountValue));
this.emit(this._Freeze, msg.sender(), amount);
}
unfreeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._freezes.set(account, this._freezes.get(account).sub(amountValue));
this._balances.set(account, this._balances.get(account).add(amountValue));
this.emit(this._Unfreeze, msg.sender(), amount);
}
freezeOf(account) {
return this._freezes.get(address(account));
}
}
class APL20LOCK extends APL20FREEZE {
_locks = Mapping.UBN();
_releaseTime = 0;
constructor(name, symbol, cap, initialSupply, rate, releaseTime, vault) {
super(name, symbol, cap, initialSupply, rate, vault);
if (isTraceEnabled()) {
console.log(`APL20LOCK: arguments releaseTime=${releaseTime}, initialSupply=${initialSupply}`);
}
const isoReleaseTime = toTimestamp(releaseTime);
if (isTraceEnabled()) {
console.log(`APL20LOCK: _releaseTime=${isoReleaseTime}, block.timeStamp=${block.timestamp()}`);
}
require(isoReleaseTime / 1000 > block.timestamp(), "APL20Lockable: release time is before current block time");
this._releaseTime = isoReleaseTime;
}
releaseTime() {
return this._releaseTime;
}
buy() {
const amount = super.buy();
const account = msg.sender();
this._locks.set(account, this._locks.get(account).add(amount));
return amount;
}
unlock() {
const account = msg.sender();
const amount = this.lockOf(account);
if (isTraceEnabled()) {
console.log(`APL20LOCK: unlock releaseTime=${this._releaseTime} block.timestamp()=${block.timestamp()}`);
}
require(block.timestamp() >= this.releaseTime() / 1000, "APL20Lockable: current time is before release time");
this._transfer(address(self), account, amount);
this._locks.set(account, UBN.ZERO);
return true;
}
lockOf(account) {
return this._locks.get(address(account));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per APL)`);
console.log(`| cap: ${fromAtm(this.cap())} `);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log(`| vault: ${this.vault().getHex()}`);
console.log(`| r. timestamp: ${this.releaseTime() / 1000} in seconds`);
console.log(`| release time: ${new Date(this.releaseTime()).toISOString()}`);
console.log("|---------------------------------------------------------------");
console.log(`|blk.timestamp: ${new Date(block.timestamp() * 1000).toISOString()}`);
console.log(`| current time: ${new Date().toISOString()}`);
console.log("|---------------------------------------------------------------\\n");
}
}
class MyAPL20LOCK extends APL20LOCK {
constructor(){
super(''Deal2'',''ntk'',''100000000000000'',''100000000000000'',''1000000000'',''2021-09-23T15:22:00.167Z'',''0x54cb3319380bdb3a'');
}
}', 'MyAPL20LOCK', 'APL20LOCK', '', 'js', '0.1.1', 'ACTIVE', 294, 1)
;#
insert into smc_contract (db_id, address, owner, transaction_id, data, name, base_contract, args, language, version,
                          status, height, latest)
VALUES (3, 8194546512095429093, -208393164898941117, 6215280239318408678, '
class Contract {
emit(event, ...params) {
if (isTraceEnabled()) {
Contract.discoveryObject(event, false);
}
require(typeof event !== "undefined", "Missing event type.")
require(Java.isJavaObject(event), "Wrong event type.")
EventManager.emit(event, params);
}
static deserialize(dataString) {
let dataObject = JSON.parse(dataString);
if (dataObject && dataObject.meta) {
if (dataObject.meta.create) {
try {
const values = dataObject.meta.args?.map(arg => dataObject[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className} creator=${dataObject.meta.create} args=${values}`);
}
dataObject = eval(dataObject.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + dataObject.meta.create);
throw e;
}
} else if (dataObject.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${dataObject.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(dataObject.meta.className);
} catch (e) {
console.log(''Class not found: '' + dataObject.meta.className);
throw e;
}
if (originalClass && originalClass.prototype) {
const oldMainClassObjProto = originalClass.prototype;
const deepParse = function (classObj, partDataObject) {
if (classObj && classObj.prototype) {
const oldProto = classObj.prototype;
const classNew = function () {
for (const field in partDataObject) {
let fieldData = partDataObject[field];
if (fieldData && typeof fieldData === ''object'' && ''meta'' in fieldData) {
if (fieldData.meta.create) {
try {
const values = fieldData.meta.args?.map(arg => fieldData[arg]) || [];
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className} creator=${fieldData.meta.create} args=${values}`);
}
this[field] = eval(fieldData.meta.create)(...values);
} catch (e) {
console.log(''Can not call method: '' + fieldData.meta.create);
throw e;
}
} else if (fieldData.meta.className) {
if (isTraceEnabled()) {
console.log(`deserialize: className=${fieldData.meta.className}`);
}
let originalClass = null;
try {
originalClass = eval(fieldData.meta.className);
} catch (e) {
console.log(''Class not found: '' + fieldData.meta.className);
throw e;
}
this[field] = new deepParse(originalClass, fieldData);
}
} else {
this[field] = fieldData;
}
}
};
classNew.prototype = oldProto;
return new classNew();
} else {
return partDataObject;
}
};
deepParse.prototype = oldMainClassObjProto;
return new deepParse(originalClass, dataObject);
} else {
return dataObject;
}
}
} else {
return dataObject;
}
}
static discoveryObject(object, elements) {
console.log(`***Got object=${object} type=${typeof object} isJavaObject=${Java.isJavaObject(object)}`)
if (typeof object === "object" && elements) {
for (const element in object) {
console.log(`*** element=${element} value=${object[element]}`)
}
}
}
}
class APL20 extends Contract {
_balances = Mapping.UBN();
_allowances = Mapping.UBN();
_totalSupply = new UBN();
_name = '''';
_symbol = '''';
_Transfer = new Event("Transfer:from,to,amount", 2);
_Approval = new Event("Approval:owner,spender,amount", 2);
constructor(name, symbol) {
super();
require(name !== undefined && symbol !== undefined, "APL20: constructor undefined parameter");
this._name = name;
this._symbol = symbol;
}
name() {
return this._name;
}
symbol() {
return this._symbol;
}
decimals() {
return new UBN(''8'');
}
totalSupply() {
return this._totalSupply;
}
balanceOf(account) {
const addr = address(account);
return this._balances.get(addr);
}
transfer(recipient, amount) {
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(msg.sender(), recipientAddr, value);
return true;
}
allowance(owner, spender) {
const ownerAddr = address(owner);
const spenderAddr = address(spender);
return this._allowances.get(ownerAddr, spenderAddr);
}
approve(spender, amount) {
const spenderAddr = address(spender);
const value = new UBN(amount);
this._approve(msg.sender(), spenderAddr, value);
return true;
}
transferFrom(sender, recipient, amount) {
const senderAddr = address(sender);
const recipientAddr = address(recipient);
const value = new UBN(amount);
this._transfer(senderAddr, recipientAddr, value);
this._approve(senderAddr, msg.sender(), this._allowances.get(senderAddr, msg.sender()).sub(value));
return true;
}
increaseAllowance(spender, addedValue) {
const spenderAddr = address(spender);
const value = new UBN(addedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).add(value));
return true;
}
decreaseAllowance(spender, subtractedValue) {
const spenderAddr = address(spender);
const value = new UBN(subtractedValue);
this._approve(msg.sender(), spenderAddr, this._allowances.get(msg.sender(), spenderAddr).sub(value));
return true;
}
_transfer(senderAddr, recipientAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===TRANSFER: sender=${senderAddr}, recipient=${recipientAddr}, amount=${amountValue}`);
}
const sender = address(senderAddr);
const recipient = address(recipientAddr);
const amount = new UBN(amountValue);
require(sender && sender !== address(''0x0''), "APL20: transfer from the zero address");
require(recipient && recipient !== address(''0x0''), "APL20: transfer to the zero address");
if (isTraceEnabled()) {
console.log(`===TRANSFER: 1: sender balance=${this._balances.get(sender)}`);
}
require(this._balances.get(sender).gte(amount), "APL20: transfer amount exceeds balance");
this._balances.set(sender, this._balances.get(sender).sub(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: sender balance=${this._balances.get(sender)}`);
console.log(`===TRANSFER: 1: recipient balance=${this._balances.get(recipient)}`);
}
this._balances.set(recipient, this._balances.get(recipient).add(amount));
if (isTraceEnabled()) {
console.log(`===TRANSFER: 2: recipient balance=${this._balances.get(recipient)}`);
}
this.emit(this._Transfer, sender, recipient, amount);
}
_mint(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===MINT: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== address(''0x0''), "APL20: mint to the zero address");
this._totalSupply = this._totalSupply.add(amount);
if (isTraceEnabled()) {
console.log(`===MINT: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).add(amount));
if (isTraceEnabled()) {
console.log(`===MINT: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, ADDRESS_ZERO, account, amount);
}
_burn(accountAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===BURN: account=${accountAddr}, amount=${amountValue} sender=${msg.sender()}`);
}
const account = address(accountAddr);
const amount = new UBN(amountValue);
require(account !== ADDRESS_ZERO, "APL20: burn from the zero address");
this._totalSupply = this._totalSupply.sub(amount);
if (isTraceEnabled()) {
console.log(`===BURN: 1: account balance=${this._balances.get(account)}`);
}
this._balances.set(account, this._balances.get(account).sub(amount));
if (isTraceEnabled()) {
console.log(`===BURN: 2: account balance=${this._balances.get(account)}`);
}
this.emit(this._Transfer, account, ADDRESS_ZERO, amount);
}
_approve(ownerAddr, spenderAddr, amountValue) {
if (isTraceEnabled()) {
console.log(`===APROVE: owner=${ownerAddr}, spender=${spenderAddr}, value=${amountValue}, sender=${msg.sender()}`);
}
const spender = address(spenderAddr);
const owner = address(ownerAddr);
const amount = new UBN(amountValue);
require(owner !== ADDRESS_ZERO, "APL20: approve from the zero address");
require(spender !== ADDRESS_ZERO, "APL20: approve to the zero address");
this._allowances.set(owner, spender, amount);
this.emit(this._Approval, owner, spender, amount)
}
}
class APL20CAP extends APL20 {
_cap = new UBN();
constructor(name, symbol, cap) {
super(name, symbol);
const capValue = new UBN(cap)
require(capValue.gt(UBN.ZERO), "APL20CAP: cap is 0");
this._cap = capValue;
}
cap() {
return this._cap;
}
_mint(account, amount) {
const accountAddr = address(account);
const amountValue = new UBN(amount);
require(this.totalSupply().add(amountValue).lte(this._cap), "APL20CAP: cap exceeded");
super._mint(accountAddr, amountValue);
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| cap: ${fromAtm(this.cap())}`);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BURN extends APL20CAP {
burn(amount) {
this._burn(msg.sender(), new UBN(amount));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20BUY extends APL20BURN {
_rate = new UBN(1);
_vault = address(''0x0'');
_totalBuy = new UBN();
_Buy = new Event("Buy:who,amount", 1);
constructor(name, symbol, cap, initialSupply, rate, vault) {
super(name, symbol, cap);
this._rate = new UBN(rate);
this._vault = address(vault);
super._mint(address(self), new UBN(initialSupply));
}
vault() {
return this._vault;
}
rate() {
return this._rate;
}
buy() {
let amount = msg.value().mul(this._rate).div(APL);
this._totalBuy.add(amount);
require(this._totalBuy.lte(this._totalSupply), "APL20BUY: exceeded the total buy amount.");
address(this._vault).transfer(msg.value());
this.emit(this._Buy, msg.sender(), amount)
return amount;
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per apl)`);
console.log(`| total supply: ${this.totalSupply()} atm`);
console.log(`| total buy: ${this._totalBuy} atm`);
console.log("|---------------------------------------------------------------\\n");
}
}
class APL20FREEZE extends APL20BUY {
_freezes = Mapping.UBN();
_Freeze = new Event(''Freeze:address,amount'', 1);
_Unfreeze = new Event(''Unfreeze:address,amount'', 1);
freeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._balances.set(account, this._balances.get(account).sub(amountValue));
this._freezes.set(account, this._freezes.get(account).add(amountValue));
this.emit(this._Freeze, msg.sender(), amount);
}
unfreeze(amount) {
const amountValue = new UBN(amount);
const account = msg.sender();
this._freezes.set(account, this._freezes.get(account).sub(amountValue));
this._balances.set(account, this._balances.get(account).add(amountValue));
this.emit(this._Unfreeze, msg.sender(), amount);
}
freezeOf(account) {
return this._freezes.get(address(account));
}
}
class APL20LOCK extends APL20FREEZE {
_locks = Mapping.UBN();
_releaseTime = 0;
constructor(name, symbol, cap, initialSupply, rate, releaseTime, vault) {
super(name, symbol, cap, initialSupply, rate, vault);
if (isTraceEnabled()) {
console.log(`APL20LOCK: arguments releaseTime=${releaseTime}, initialSupply=${initialSupply}`);
}
const isoReleaseTime = toTimestamp(releaseTime);
if (isTraceEnabled()) {
console.log(`APL20LOCK: _releaseTime=${isoReleaseTime}, block.timeStamp=${block.timestamp()}`);
}
require(isoReleaseTime / 1000 > block.timestamp(), "APL20Lockable: release time is before current block time");
this._releaseTime = isoReleaseTime;
}
releaseTime() {
return this._releaseTime;
}
buy() {
const amount = super.buy();
const account = msg.sender();
this._locks.set(account, this._locks.get(account).add(amount));
return amount;
}
unlock() {
const account = msg.sender();
const amount = this.lockOf(account);
if (isTraceEnabled()) {
console.log(`APL20LOCK: unlock releaseTime=${this._releaseTime} block.timestamp()=${block.timestamp()}`);
}
require(block.timestamp() >= this.releaseTime() / 1000, "APL20Lockable: current time is before release time");
this._transfer(address(self), account, amount);
this._locks.set(account, UBN.ZERO);
return true;
}
lockOf(account) {
return this._locks.get(address(account));
}
trace() {
console.log("|---------------------------------------------------------------");
console.log(''| Token:'');
console.log(`| name: ${this.name()}`);
console.log(`| symbol: ${this.symbol()}`);
console.log(`| decimals: ${this.decimals()}`);
console.log(`| rate: ${fromAtm(this.rate())} (tokens per APL)`);
console.log(`| cap: ${fromAtm(this.cap())} `);
console.log(`| total supply: ${this.totalSupply()} ATM`);
console.log(`| vault: ${this.vault().getHex()}`);
console.log(`| r. timestamp: ${this.releaseTime() / 1000} in seconds`);
console.log(`| release time: ${new Date(this.releaseTime()).toISOString()}`);
console.log("|---------------------------------------------------------------");
console.log(`|blk.timestamp: ${new Date(block.timestamp() * 1000).toISOString()}`);
console.log(`| current time: ${new Date().toISOString()}`);
console.log("|---------------------------------------------------------------\\n");
}
}
class MyAPL20LOCK extends APL20LOCK {
constructor(){
super(''SC TEST'',''SC TEST'',''100000000000000'',''100000000000000'',''1000000000'',''2021-09-23T09:31:00.413Z'',''0xd93e7865b61ad38a'');
}
}', 'MyAPL20LOCK', 'APL20LOCK', '', 'js', '0.1.1', 'ACTIVE', 337, 1)
;#