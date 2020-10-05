DELETE FROM update_status;
DELETE FROM `trim`;
DELETE FROM transaction;
DELETE FROM block;
DELETE FROM two_factor_auth;
DELETE FROM account;
DELETE FROM version;
DELETE FROM transaction_shard_index;
delete from block_index;
delete from option;
delete from shard;
delete from referenced_transaction;
delete from phasing_poll;
delete from phasing_poll_result;
delete from phasing_poll_voter;
delete from phasing_vote;
delete from phasing_poll_linked_transaction;
delete from genesis_public_key;
delete from public_key;
TRUNCATE TABLE shard_recovery;
delete from tagged_data_timestamp;
TRUNCATE TABLE data_tag;
delete from tagged_data_extend;
delete from tagged_data;
TRUNCATE TABLE goods;
TRUNCATE TABLE purchase;
TRUNCATE TABLE tag;
TRUNCATE TABLE purchase_feedback;
TRUNCATE TABLE purchase_public_feedback;
delete from account_control_phasing;
delete from shuffling_data;
TRUNCATE TABLE prunable_message;
delete from phasing_approval_tx;
delete from dex_offer;
delete from mandatory_transaction;
delete from dex_contract;
delete from dex_transaction;
TRUNCATE TABLE user_error_message;
delete from account_info;
delete from asset;
delete from asset_delete;
delete from asset_dividend;
delete from asset_transfer;
delete from buy_offer;
delete from sell_offer;
delete from exchange;
delete from exchange_request;

INSERT INTO block
(DB_ID,         ID,                HEIGHT,      VERSION,   `TIMESTAMP`,  PREVIOUS_BLOCK_ID,  TOTAL_AMOUNT,        TOTAL_FEE,   PAYLOAD_LENGTH,   PREVIOUS_BLOCK_HASH,                                                   CUMULATIVE_DIFFICULTY,  BASE_TARGET,    NEXT_BLOCK_ID,               GENERATION_SIGNATURE,                                                   BLOCK_SIGNATURE,                                                                                                                        PAYLOAD_HASH,                                                           GENERATOR_ID,       TIMEOUT) VALUES
(1	        ,-107868771406622438   ,0	        ,-1         ,0	        , null                  ,0	            ,0	                ,0          ,X'0000000000000000000000000000000000000000000000000000000000000000'	,X'00'	                ,5124095	    , 8235640967557025109		,X'bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5'	,X'00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'	,X'0000000000000000000000000000000000000000000000000000000000000000'	, 1739068987193023818	,0   ),
(104830	    ,-468651855371775066   ,1000	 	,3	        ,9200       , 9108206803338182346   ,0	            ,100000000	        ,1255       ,X'cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282'    ,X'026543d9a8161629'    ,9331842	    ,-1868632362992335764	    ,X'002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf'	,X'e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31'    ,X'37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0'	, 9211698109297098287   ,0   ),
(105015	    ,-7242168411665692630  ,1500	 	,3	        ,13800      ,-3475222224033883190   ,0	            ,100000000	        ,1257       ,X'cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0'    ,X'026601a7a1c313ca'    ,7069966	    , 5841487969085496907		,X'fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef'	,X'978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e'    ,X'2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6'	, 9211698109297098287   ,0   ),
(1641706	,-6746699668324916965  ,2000	 	,5	        ,18400      , 2069655134915376442	,0	            ,200000000	        ,207	    ,X'3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e'	,X'02dfb5187e88edab'	,23058430050	,-3540343645446911906		,X'dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b'	,X'4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4'	,X'18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99'	, 5564664969772495473	,1   ),
(1641707	,-3540343645446911906  ,2499	 	,6	        ,22998      ,-6746699668324916965	,0	            ,0	                ,0	        ,X'1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e'	,X'02dfb518ae37f5ac'	,23058430050	, 2729391131122928659		,X'facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3'	,X'f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-902424482979450876	,4   ),
(1641708	, 2729391131122928659  ,2998	 	,6	        ,28098      ,-3540343645446911906	,0	            ,0	                ,0	        ,X'5e485346362cdece52dada076459abf88a0ae128cac6870e108257a88543f09f'	,X'02dfb518dde6fdad'	,23058430050	, 1842732555539684628		,X'1fc63f083c3a49042c43c22a7e4d92aadac95c78d06ed21b8f9f0efd7c23b2a1'	,X'8cb8795d7a320e693e64ea67b47348f2f1099c3e7163311b59135aaff1a78a00542b9d4713928c265666997a4ce63b0c07585ce04464f8dfb2253f21f91bf22e'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 4363726829568989435	,3   ),
(1641709	, 1842732555539684628  ,3500	 	,4	        ,32200      , 2729391131122928659	,0	            ,200000000	        ,207	    ,X'130cafd7c5bee025885d0c6b58b2ddaaed71d2fa48423f552eb5828a423cc94b'	,X'02dfb5190d9605ae'	,23058430050	,-5580266015477525080		,X'a042a2accbb2600530a4df46db4eba105ac73f4491923fb1c34a6b9dd2619634'	,X'ee4e2ccd12b36ade6318b47246ddcad237a153da36ab9ea2498373a4687c35072f2a9d49925520b588cb16d0e5663f3d10e3adeee97dcbbb4137470e521b347c'	,X'9a8d7e4f2e83dc49351f9c3d72fabc5ecdc75f6eccc2b90f147ff5ec7d5068b2'	, 4363726829568989435	,0   ),
(1641710	,-5580266015477525080  ,5000	 	,6	        ,46000      , 1842732555539684628	,0	            ,0	                ,0	        ,X'149dfdfc7eb39219330d620a14fb0c2f02369abbda562bc4ab068e90c3cf11a4'	,X'02dfb5193d450daf'	,23058430050	, 6438949995368593549		,X'20feb26c8c34c22d55de747e3964acb3bc864326736949876d2b0594d15e87dd'	,X'a22f758567f0bd559ce2d821399da4f9ffdc4a694057d8b37045d2a9222be405f4311938e88a0b56418cbadcbea47dadabfc16e58f74e5dcd7a975d95dc17766'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-6535098620285495989	,7   ),
(1641711	, 6438949995368593549  ,8000	 	,4	        ,73600      ,-5580266015477525080	,0	            ,200000000	        ,207	    ,X'a81547db9fe98eb224d3cdc120f7305d3b829f162beb3bf719750e0cf48dbe9d'	,X'02dfb5196cf415b0'	,23058430050	, 7551185434952726924		,X'5b1bf463f202ec0d4ab42a9634976ed47b77c462d1de25e3fea3e8eaa8add8f6'	,X'992eacb8ac3bcbb7dbdbfcb637318adab190d4843b00da8961fd36ef60718f0f5acca4662cfdcf8447cc511d5e36ab4c321c185382f3577f0106c2bfb9f80ee6'	,X'8bdf98fbc4cfcf0b66dfaa688ce7ef9063f8b1748ee238c23e8209f071cfcee7'	, 6415509874415488619	,0   ),
(1641712	, 7551185434952726924  ,10000 	    ,6	        ,92000      , 6438949995368593549	,0	            ,0	                ,0	        ,X'8db872e0e7be5b59fb68ef26d84bfeb9df04f6a5b6f701fd1c88578bfcf48a84'	,X'02dfb5199ca31db1'	,23058430050	, 8306616486060836520		,X'1435d4b603b52d04dd0f8228f36dbd6f01e627a59370fa3e6a0f58a75b372621'	,X'5a8acc3cc947b76d42fa78938ed9ece33b91c5ca0bb7a1af6c92ec525e8bb6092babf03aee10bd965123fceb5afad63969e78991d8c6b2a6b4fc79cff8fe150d'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 7160808267188566436	,6   ),
(1641713	, 8306616486060836520  ,15000 	    ,6	        ,138000     , 7551185434952726924	,0	            ,0	                ,0	        ,X'8cf9752b2533cb6849ad83b275c40f7e61b204ac023f775847a60c2f1a9d3d79'	,X'02dfb519cc5225b2'	,23058430050	,-6206981717632723220		,X'5f1382ab768b8b000637d8a59d7415fd8d4b6d4edc00ca0a08aacf9caf8c9a06'	,X'6832f74bd4abe2a4b95755ff9e989133079e5215ae2111e590ea489353ce28078d094db3db077124ac541be9f4f7f09f5a36aac83c8c151dae0f09eb378033e1'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-3985647971895643754	,9   ),
(1641714	,-6206981717632723220  ,15456 	    ,4	        ,142195     , 8306616486060836520	,0	            ,200000000	        ,207	    ,X'a8460f09af074773186c58688eb29215a81d5b0b10fc9e5fc5275b2f39fd93bb'	,X'02dfb519fc012db3'	,23058430050	,-4166853316012435358		,X'df545469ed5a9405e0ff6efcdf468e61564776568c8b227f776f24c47206af46'	,X'3d1c22000eb41599cb12dfbfaa3980353fa84cdf99145d1fcc92886551044a0c0b388c539efa48414c21251e493e468d97a2df12be24e9a33dec4521fdb6c2eb'	,X'550dfe6da8732c1977c7545675f8dc163995aaba5533306b7a1f1b9364190dd3'	, 4749500066832760520	,0   ),
(1641715	,-4166853316012435358  ,104595 	    ,6	        ,962274     ,-6206981717632723220	,0	            ,0	                ,0	        ,X'ec562889035fdca9d59d9bdca460992c01c5286278104287a989834eeffcb83e'	,X'02dfb51a2bb035b4'	,23058430050	, 433871417191886464		,X'82e59d851fdf0d01ca1ee20df906009cd66885cc63e8314ebde80dc5e38987fa'	,X'202acda4d57f2a24212d265053241a07608de29a6dd8252994cf8be197765d02a585c676aca15e7f43a57d7747173d51435d9f2820da637ca8bc9cd1e536d761'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 3883484057046974168	,9   ),
(1650020	,6282714800700403321   ,517468	    ,5	        ,41571157	,-3194395162061405253	,12000000000    ,23000000000	    ,414	    ,X'bb831a55863aabd3d2622a1692a4c03ba9eb14839902e029a702c58aeea6a935'	,X'3d46b0302ef95c'      ,7686143350	    ,-5966687593234418746       ,X'd60150d67b47f37a90ca0b0c7a0151af1c2d9a69687f3eef75f42d7b5f12c191'	,X'd2c6b60abaf85e17f65f339879fda8de5346415908a9cbb9a21b3c6d24bd1d0454222fb8962ad2aec679da0d8fb7e835b76a35301c33e925b48245a9d24954de'	,X'4555a1d9a7c2226b9a5797e56d245485cb94fdb2495fc8ca31c3297e597c7b68'	,9211698109297098287	,2   ),
(1800000	,-5966687593234418746	,553326	    ,3	        ,41974339	,-420771891665807004	,0	            ,1000000000	        ,2668	    ,X'6459caa1311e29fa9c60bed5752f161a5e82b77328cac949cb7afbaccacfbb8e'	,X'3de7206ceaebce'	    ,168574215	    , null              	    ,X'dc3b7c24f1e6caba84e39ff7b8f4040be4c614b16b7e697364cedecdd072b6df'	,X'866847568d2518e1c1c6f97ee014b6f15e4197e5ff9041ab449d9087aba343060e746dc56dbc34966d42f6fd326dc5c4b741ae330bd5fa56539022bd75643cd6'	,X'cf8dc4e015626b309ca7518a390e3e1e7b058a83428287ff39dc49b1518df50c'	,-208393164898941117	,0)
  ;

--last block from testnet1;

INSERT INTO transaction
(DB_ID,     ID,                      HEIGHT,      BLOCK_ID,            BLOCK_TIMESTAMP,    DEADLINE, RECIPIENT_ID,     TRANSACTION_INDEX, AMOUNT,             FEE,            FULL_HASH,                                                                     SIGNATURE,                                                                                                                                  `TIMESTAMP`, TYPE, SUBTYPE, SENDER_ID,                SENDER_PUBLIC_KEY,                                                   REFERENCED_TRANSACTION_FULL_HASH,                                                     PHASED, VERSION, HAS_MESSAGE, HAS_ENCRYPTED_MESSAGE, HAS_PUBLIC_KEY_ANNOUNCEMENT, EC_BLOCK_HEIGHT,   EC_BLOCK_ID,            HAS_ENCRYPTTOSELF_MESSAGE, HAS_PRUNABLE_MESSAGE, HAS_PRUNABLE_ENCRYPTED_MESSAGE, HAS_PRUNABLE_ATTACHMENT, ATTACHMENT_BYTES) VALUES
  (150      ,3444674909301056677	  ,1000	     ,-468651855371775066       ,9200	       ,1440,	null	                ,0	        ,0	                ,2500000000000	,X'a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	      ,X'375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc'	    ,35073712	    ,5	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	                ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14399	        ,-5416619518547901377	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 X'01056673646673035145520500667364667301ae150000000000000000000000000000ae150000000000000000000000000000000000000000000001'),
  (175      ,2402544248051582903	  ,1000	     ,-468651855371775066       ,9200	       ,1440,	null	                ,1	        ,0	                ,1000000000000	,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'	      ,X'fc6f11f396aa20717c9191a1fb25fab0681512cc976c935db1563898aabad90ffc6ced28e1b8b3383d5abb55928bbb122a674dc066ab8b0cc585b9b4cdbd8fac'	    ,35075179	    ,2	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'6500000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	                ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14405	        ,-2297016555338476945	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 X'01074d5941535345540b00666466736b64666c616c73102700000000000002'),
  (200      ,5373370077664349170	  ,1500	     ,-7242168411665692630      ,13800	       ,1440,	457571885748888948	    ,0	        ,100000000000000000	,100000000	    ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	      ,X'8afd3a91d0e3011e505e0353b1f7089c0d401672f8ed5d0ddc2107e0b130aa0bdd17f03b2d75eed8fcc645cda88b5c82ac1b621c142abad9b1bb95df517aa70c'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (500      ,-780794814210884355	  ,2000	     ,-6746699668324916965      ,18400	       ,1440,	6110033502865709882	    ,1	        ,100000000000000000	,100000000	    ,X'fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558'	      ,X'240b0a1ee9f63f5c3cb914b42584da1388b9d048a981f1651ac85dd12f12660c29782100c03cbe8491bdc831aa27f6fd3a546345b3da7860c56e6ba431550517'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	                ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (1000     ,-9128485677221760321	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -603599418476309001	    ,0	        ,100000000000000000	,100000000	    ,X'bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1'	      ,X'75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (1500     ,3746857886535243786	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -693728062313138401	    ,1	        ,100000000000000000	,100000000	    ,X'0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3'	      ,X'73a84f612f5957453b502ae8aeaa31bc2add030b1b9182624c66eb94d6377000462286b22ca7fcd6e13987292858b02b0b14ac4539b97df4bd3b14303797f11b'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (2000     ,5471926494854938613	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -3934231941937607328     ,2	        ,100000000000000000	,100000000	    ,X'f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691'	      ,X'98f5fc631ea607b47bf7888eb3253e0e696f5fd4bf26d6c698a9c69e1078ab0ff7afc6e76c5b1043f6ff00ecea2fed75c83dcbac754c195f29a61a6632010a39'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,TRUE,                 null),
  (2500     ,2083198303623116770	  ,8000	     , 6438949995368593549      ,73600	       ,1440,  -1017037002638468431     ,0	        ,100000000000000000	,100000000	    ,X'e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f'	      ,X'd24811bc4be2c7031196fd220639f1885c8e15c96e7672146c88c2eea25d8a0cd4e93b8e2324e2522e3aff14faa1ef811fc43a971fdbdb71f7ac0b5614e706cb'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (3000     ,808614188720864902	      ,8000	     , 6438949995368593549      ,73600	       ,1440,  -5803127966835594607     ,1	        ,100000000000000000	,100000000	    ,X'863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6'	      ,X'38484c6128b2707a81ea6f0c9f19663dbcd54358e644d56cfa2b33635f2d570f7b91c41820f8d1923e0afca5cb0e5785c76c2fd859e354c876a9640a75882aa2'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (3500     ,-2262365651675616510	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	2569665864951373924	    ,0	        ,100000000000000000	,100000000	    ,X'026bd4236d769ae022df97e248c6292aef1f403f5d5dcb74d787255344cf58e5'	      ,X'1a3ecfc672df4ae91b1bcf319cee962426cd3f65fac340a0e01ac27367646904fa8ccf22f0b0c93f84d00584fa3f7f5bd03933e08b3aa1295a9ebdd09a0c1654'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (4000     ,9145605905642517648	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	2230095012677269409	    ,1	        ,100000000000000000	,100000000	    ,X'9074899d1db8eb7e807f0d841973fdc8a84ab2742a4fb03d47b620f5e920e5fe'	      ,X'6ae95b4165ef53b335ac576a72d20d24464f57bd49dbdd76dd22f519caff3d0457d97769ae76d8496906e4f1ab5f7db30db73daea5db889d80e1ac0bd4b05257'	    ,35078474	    ,0	   ,0	    ,-8315839810807014152	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,null	                                                                                ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (4500     ,-1536976186224925700     ,15456	 ,-6206981717632723220      ,142195    	   ,1440,	null	                ,2	        ,0	                ,100000000	    ,X'fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f'	      ,X'61a224ae2d8198bfcee91c83e449d6325a2caa974f6a477ab59d0072b9b7e50793575534ab29c7be7d3dbef46f5e9e206d0bf5801bebf06847a28aa16c6419a1'	    ,36758888	    ,8	   ,1	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088'	                ,FALSE      ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,103874	        ,1281949812948953897	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,	                X'01054c494e5558035838360002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4'),
  (5000     ,-4081443370478530685	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	null	                ,3	        ,0	                ,100000000	    ,X'830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088'	      ,X'551f99bc4eceaae7c7007ac077ed163f4d95f8acc0119e38b726b5c8b494cf09c5059292de17efbc4ec14848e3944ecd0a5d0ca2591177266e04d426ce25a1c1'      ,36763004	    ,8	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,null	                                                                                ,FALSE      ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,103950	        ,3234042379296483074	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,	                X'01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4'),
  (6000 	,4851834545659781120	  ,517468    ,6282714800700403321       ,41571157      ,1440,   7477442401604846627	    ,0	        ,12000000000	    ,23000000000	,X'0020052bd02d5543c4408aed90d98e636fdb21447cbed0c1f1e2db3134e37fbf'		  ,X'7ace0ea75778aebb8363e141da74b4efce571dc73c728de7f3bcd6126fe3ab04fb1b8e3170e6fe4e458f9fd40f8d10ef7bc8caa839ae9c28a2276f02ddccd2ff'	    ,41571172	    ,0	   ,0	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,null	                                                                                ,TRUE		,1	,TRUE	        ,FALSE	            ,FALSE	                        ,516746	        ,5629144656878115682	,TRUE	                    ,FALSE	                ,TRUE	                        ,FALSE,                 X'010c00008054657374206d65737361676501400000808bb31f0eb60af644d69bad77c5158ceac89bb3b02856542f334de903be92ad354d11f1f5eb876d3e558c40513c813248a879751d03d6446d6c562e04306573f6adcb4a9238585b1f9f1df4c124055da5ba78d76521eb2ace178f552d064a2cf802a83108000232000000000000000a0000000000000002dc3fd47da87a5620983fe492a3968c6c93931ffe397ff94202000000ffffffff019fec636832fa9108934bac4902b7bd9213f4c0f073625dcdc9a2c511cc715fdc'),
  (7000	    ,9175410632340250178	  ,553326	 ,-5966687593234418746	    ,41974339	   ,1440,	null	                ,0	        ,0	                ,1000000000	    ,X'429efb505b9b557f5d2a1d6d506cf75de6c3692ca1a21217ae6160c7658c7312'	      ,X'7ecae5825a24dedc42dd11e2239ced7ad797c6d6c9aedc3d3275204630b7e20832f9543d1063787ea1f32ab0993ea733aa46a52664755d9e54f211cdc3c5c5fd'	    ,41974329	    ,3	   ,0	    ,3705364957971254799	 ,X'39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152' ,null	                                                                                ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,552605	        ,4407210215527895706	,FALSE	                    ,TRUE	                ,FALSE	                        ,FALSE,	                X'010c00546573742070726f647563741500546573742070726f6475637420666f722073616c650c007461672074657374646174610200000000e40b540200000001b9dd15475e2f8da755f1b63933051dede676b223c86e70f54c7182b976d2f86d')
;

--last tx from testnet1;

INSERT INTO update_status (transaction_id, updated) VALUES (
  -1536976186224925700, TRUE
);

insert into account
(DB_ID  	,ID  	                        ,BALANCE  	        ,UNCONFIRMED_BALANCE  	,HAS_CONTROL_PHASING  	,FORGED_BALANCE  	,ACTIVE_LESSEE_ID  	,HEIGHT  	,LATEST, DELETED) values
(10          ,50                            ,555500000000       ,105500000000           ,false                  , 0                 ,null               ,100000     ,TRUE     , false),
(20          ,100                           ,100000000          , 100000000             ,false                  ,0                  ,null               ,104595     ,true     , false),
(30          ,200                           , 250000000         , 200000000             ,false                  ,0                  , null              ,104670     ,true     , false),
(40          ,7821792282123976600           , 15025000000000    , 14725000000000        ,false                  ,0                  , null              ,105000     ,true     , false),
(50          ,9211698109297098287           , 25100000000000    , 22700000000000        ,false                  ,0                  , null              ,106000     ,true     , false),
(60          ,500                           ,77182383705332315  ,77182383705332315      ,false                  ,0                  ,null               ,141839     ,false    , false),
(70          ,500                           ,77216366305332315  ,77216366305332315      ,false                  ,0                  ,null               ,141844     ,false    , false),
(80          ,500                           ,77798522705332315  ,77798522705332315      ,false                  ,0                  ,null               ,141853     ,true     , false),
(90          ,600                           ,40767800000000     ,40767800000000         ,false                  ,0                  ,null               ,141855     ,false    , false),
(100         ,600                           ,41167700000000     ,41167700000000         ,false                  ,0                  ,null               ,141858     ,true     , false),
(110         ,700                           ,2424711969422000   ,2424711969422000       ,false                  ,1150030000000      ,null               ,141860     ,true     , false),
(120         ,800                           ,2424711869422000   ,2424711869422000       ,false                  ,1150030000000      ,null               ,141862     ,false    , false),
(130         ,800                           ,2424711769422000   ,2424711769422000       ,false                  ,1150030000000      ,null               ,141864     ,false    , false),
(140         ,800                           ,77200915499807515  ,77200915499807515      ,false                  ,0                  ,null               ,141866     ,false    , true ),
(150         ,800                           ,40367900000000     ,40367900000000         ,false                  ,0                  ,null               ,141868     ,false    , true )
;

INSERT INTO two_factor_auth (account, secret, confirmed) VALUES
(100, X'a3f312570b65671a7101', true),
(200, X'f3e0475e0db85a822037', false)
;
INSERT into block_index (block_id, block_height) VALUES
(3, 30),
(1, 1),
(2, 2)
;
INSERT into transaction_shard_index(transaction_id, partial_transaction_hash, height, transaction_index) VALUES
(100,X'cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e' ,30, 0),
(101,X'2270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d' ,1 , 0),
(102,X'b96d5e9f64e51c597513717691eeeeaf18a26a864034f62c' ,1 , 1),
(103,X'cca5a1f825f9b918be00f35406f70b108b6656b299755558' ,1 , 2)
;
INSERT into shard (shard_id, shard_hash, shard_height, shard_state, zip_hash_crc, generator_ids, block_timeouts, block_timestamps, prunable_zip_hash) VALUES
(1, X'8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176', 2, 0, null, '[]', '[]', '[]', null),
(2, X'a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d', 3, 100, X'a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d',
    '[782179228250, 4821792282200, 7821792282123976600]', '[0, 1]', '[45673250, 45673251]', X'0729528cd01d03c815e1aaf74e1c8950a411e0f20376881747e6ab667452d909'),
(3, X'931A8011F4BA1CDC0BCAE807032FE18B1E4F0B634F8DA6016E421D06C7E13693', 31, 50, null,
    '[57821792282, 22116981092100, 9211698109297098287]', '[1, 1]', '[45673251, 45673252]', null)
;
INSERT into referenced_transaction (db_id, transaction_id, referenced_transaction_id, height) VALUES
(10     , 100                    , 101                  ,100    ),
(20     , 101                    , 102                  ,200    ),
(30     , 102                    , 103                  ,300    ),
(40     , 3444674909301056677    , 100                  ,1000   ),
(50     , 2402544248051582903    , 101                  ,1000   ),
(60     , 5373370077664349170    , 2402544248051582903  ,1500   ),
(70     , -780794814210884355    , 5373370077664349170  ,2000   ),
(80     , -9128485677221760321   , -780794814210884355  ,3500   ),
(90     , 3746857886535243786    , -9128485677221760321 ,3500   ),
(100    , 5471926494854938613    , 3746857886535243786  ,3500   ),
(110    , 2083198303623116770    , 5471926494854938613  ,8000   ),
(120    , 808614188720864902	 ,  2083198303623116770 ,8000   ),
(130    , -2262365651675616510   , 808614188720864902   ,15456  ),
(140    , -1536976186224925700   , 808614188720864902   ,15456  )
;
INSERT INTO phasing_poll (
DB_ID  	    ,ID  	            ,ACCOUNT_ID  	    ,WHITELIST_SIZE  	,FINISH_HEIGHT  	,VOTING_MODEL  	,QUORUM  	,MIN_BALANCE  	,HOLDING_ID  	        ,MIN_BALANCE_MODEL  ,HASHED_SECRET                                                                 ,ALGORITHM  	,HEIGHT) VALUES
(10	    , 5471926494854938613	,9211698109297098287	        ,0	        ,4000	            , 5	        ,1	        ,null	        ,null	                ,0		            ,X'be65fff0fd321e40fa5857815c457669d0afdb9c3823445140a9f0a40f9d4414'           ,2	            ,3500),
(20	    , 808614188720864902	,9211698109297098287	        ,2	        ,10000	            , 0	        ,1	        ,null	        ,null	                ,0		            ,null                                                                          ,0	            ,8000),
(30	    , 2083198303623116770	,9211698109297098287	        ,0	        ,9500	            , 0	        ,1	        ,null	        ,null	                ,0		            ,null                                                                          ,0	            ,8000),
(40	    ,-4081443370478530685	,9211698109297098287	        ,0	        ,17000	            , 4	        ,3	        ,null	        ,null	                ,0		            ,null                                                                          ,0	            ,15456),
(50	    ,-1536976186224925700	,9211698109297098287	        ,1	        ,18000	            , 0	        ,3	        ,null	        ,null	                ,0		            ,null                                                                          ,0	            ,15456),
(60	    ,4851834545659781120	,9211698109297098287	        ,2	        ,537000	            , 2	        ,50	        ,10	            ,4826028362757542803	,2		            ,null                                                                          ,0	            ,517468)
;
INSERT INTO phasing_poll_result
(DB_ID  	,ID  	          ,RESULT  	,APPROVED  	,HEIGHT  ) VALUES
(10	    ,100                	,1	    ,TRUE	    ,300     ),
(20	    ,3444674909301056677	,1	    ,TRUE	    ,1500   ),
(25	    ,5471926494854938613	,1	    ,TRUE	    ,4000   ),
(30	    ,808614188720864902 	,0	    ,TRUE	    ,9000   ),
(40	    ,2083198303623116770	,0	    ,FALSE	    ,9500   )
;

INSERT into phasing_poll_voter
(DB_ID  	,TRANSACTION_ID  	,VOTER_ID  	,HEIGHT) VALUES
(20   ,808614188720864902	  , 5564664969772495473     ,8000  ),
(30   ,808614188720864902     , -8315839810807014152	,8000 ),
(40   ,-1536976186224925700   , -8315839810807014152	,15456 ),
(50   ,128                    , 102	                    ,15457 ),
(60   ,128                    , 103	                    ,15457 ),
(70   ,128                    , 104	                    ,15457 ),
(80   ,4851834545659781120	  ,2330184721294966748	    ,517468 ),
(90   ,4851834545659781120	  ,7821792282123976600	    ,517468 )
;

INSERT into phasing_vote
(DB_ID  	,VOTE_ID  	                ,TRANSACTION_ID  	   ,VOTER_ID  	,HEIGHT) VALUES
(30         ,-2262365651675616510       ,808614188720864902   ,5564664969772495473 ,8500),
(40         ,9145605905642517648        ,808614188720864902   ,-8315839810807014152	 ,8999)
;

--001100100;

INSERT into phasing_poll_linked_transaction
(DB_ID  	,TRANSACTION_ID  	    ,LINKED_FULL_HASH  	,LINKED_TRANSACTION_ID  	,HEIGHT ) VALUES
(10         ,-4081443370478530685, X'6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e', 100                   , 15456),
(20         ,-4081443370478530685, X'fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f', -1536976186224925700  , 15456),
(30         ,-4081443370478530685, X'5ea0de6146ac28b8b64d4f7f1ccbd1c7b2e43397221ef7ed3fa10c4ec0581d43', -5176698353372716962  , 15456),
(40         ,100                 , X'b273e15c07bf99b5139e5753946e004d663e83b3eadb4c8dea699ee982573ef0',  -5361043843063909454 , 15457),
(50         ,100                 , X'faf20df37f7466857d33ddcd841d535fb5b216e93104ec663454210827c155ed',  -8834245526153202950 , 15457),
(60         ,200                 , X'3a0e1742d06078d5fd2b9f3b90cb2ea861406f0bebfb7c74366c40506a7c9bb1',  -3064593098847351238 , 15458)
;

INSERT into version values (355);

--INSERT INTO FTL.INDEXES (schema, "TABLE", columns) VALUES('PUBLIC', 'CURRENCY', 'code,name,description');
--INSERT INTO FTL.INDEXES (schema, "TABLE", columns) VALUES('PUBLIC', 'TAGGED_DATA', 'NAME,DESCRIPTION,TAGS');


INSERT INTO genesis_public_key
(DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES
(1, -8446737619314270165, X'5E8D43FF197F8B554A59007F9E6F73E10BFF4DDA9906F8389D015F31D0ABC433', 1000, true),
(2, -8446683063809540616, X'25AE5107A806E561488394ED5B59916D61C2F0110182E67A1AAE19CD6BD86D0E', 1500, true),
(3, -8446656647637444484, X'7AD7781E9FD7EFD304E516374E816E09356DB8439D8D577DA02A5B3EC55D6274', 2000, true),
(4, -8446582139906358620, X'58F5675BDE9DC62EC712988E84F336C1859CE672A065706C2D6B53F24809073A', 3500, true),
(5, -8446548496587389826, X'42EDD0892C113954CC9EFD503E424E79D60D2EC94356F9C65BF7EA651F3A2E6A', 5000, true),
(6, -8446473252322443609, X'1AB245BBA7D381256999A016F40FE44EC7E3FE15EF598A3F4B089B4EBC40E973', 5000, true),
(7, -8446384352342482748, X'840544113081FD01265F6881DC4BF0627FD06E3A9D9ABF1F2FC65042AC03145D', 8000, true),
(8, -8446347883943983778, X'638EA31BC014E00B858D3EAC8CB5B1BED168EA8290B4CBAE6E6DE0498ABAD557', 8000, true),
(9, -8446281322521085217, X'2308A0680A8390ABD47F3AFE616F604047DCDD5A05E4EB1877DD9332CD56A057', 10000, true),
(10, -8446169427786754084, X'AF787EE65F2CE7355D10B3D69390BB48BBD47B725F2EB0C786F9D9E623A1AC51', 15000, true)
;


INSERT INTO public_key
(DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES
(1, 2728325718715804811, X'A3C4BF8B2CBB8863C3E30EB4590FB22839311A95CF1FD716C211AE38C7D47B33', 1000, true),
(2, -5554610658583124986, X'50D38876803FBB5EFFC9ED968245AA64A7CD217FD26DB6A60A0330E8FEF4BF61', 1500, true),
(3, 7995581942006468815, X'D36B2EC098A9349902213AB19957E544AD3D388A6593C902F5DCE2CC0556B73E', 2000, true),
(4, 4961837675843750471, X'AECFD20DAE056E71AF7273F49994602BB038B35F2CFDD14DCF770725CADF570D', 3500, true),
(5, -8549596671240600704, X'F3B7EF980A1207D55B723144C87C30740FCB4F13134F8758FA03DC2B8B086916', 5000, true),
(6, -960762611966502100, X'2FD89F2823B313FFA81E707014733939E97354FA0F69210262BD7EB1FC1E2D28', 5000, true),
(7, -6151359720024440589, X'D6896785B0D0A106B75B5268919B4D361EA32E53F2C6E93C277DB567FF7DB67D', 8000, true),
(8, -6724281675870110558, X'3BF91EC2D8ABE04B4BEDB7BF09140E31BA8B4195002D093C3BF32259FEA5A513', 8000, true),
(9, 1344527020205736624, X'5C6D82CD66A3328AA9A83C2FC2EC7724EB788A81E78ED67E2A979B127B9A887D', 10000, true),
(10, -4013722529644937202, X'6DFB3D4C9A0BC930B4700DC6C49881B71F5A48F38AFEC702BD8DE8D041CC9023', 15000, true),
(11, -208393164898941117, X'AF888EE65F2CE7355D10B3D69390BB48BBD47B725F2EB0C786F9D9E623A1AC51', 15000, true)
;

INSERT INTO shard_recovery (SHARD_RECOVERY_ID, STATE, COLUMN_NAME, UPDATED, HEIGHT) VALUES (1, 'INIT', NULL, CURRENT_TIMESTAMP(), 10000);


INSERT into tagged_data_timestamp
(DB_ID  	,ID  	             ,  `TIMESTAMP`  , HEIGHT , LATEST ) VALUES
(10         ,-780794814210884355 , 35078473      , 2000 , TRUE),
(20         ,-9128485677221760321, 35078473      , 3500, TRUE),
(30         ,3746857886535243786,  35078473      , 3500, TRUE)
;

INSERT into data_tag
(DB_ID  	,TAG      , TAG_COUNT  	,HEIGHT , LATEST) VALUES
(10         ,'abc',      1         , 1500, TRUE),
(20         ,'efd',      1         , 2000, FALSE),
(30         ,'xyz' ,     2         , 3500, FALSE),
(40         ,'trw' ,     1         , 3500, TRUE)
;

INSERT into tagged_data
(DB_ID  	,ID  	             , ACCOUNT_ID  	        , NAME  ,      description  ,tags                          , parsed_tags                 ,data              ,  is_text   ,  block_timestamp ,  transaction_timestamp , HEIGHT ) VALUES
(10         ,-780794814210884355 , 9211698109297098287  , 'tag1'  , 'tag1 descr'    ,'tag1,tag2,tag3,tag2,sl'      , '["tag1", "tag2", "tag3"]'     ,X'c11dd7986e'     ,   TRUE    ,          18400    ,        35078473        ,   2000 ),
(20         ,-9128485677221760321, 9211698109297098287  , 'tag2'  , 'tag2 descr'    ,'tag2,tag2,ss'                , '["tag2"]'                     ,X'c11d86986e'     ,   TRUE    ,          32200    ,        35078473        ,   3500 ),
(30         ,3746857886535243786 , 9211698109297098287  , 'tag3'  , 'tag3 descr'    ,'tag3,tag4,tag3,newtag'       , '["tag3", "tag4", "newtag"]'   ,X'c11d8344588e'   ,   FALSE  ,          32200    ,      35078473        ,   3500 ),
(40         ,2083198303623116770 , 9211698109297098287  , 'tag4'  , 'tag4 descr'    ,'tag3,tag3,tag3,tag2,tag2'    , '["tag3", "tag2"]'             ,X'c11d1234589e'   ,   TRUE   ,          73600    ,      35078473        ,   3500),
(50         ,808614188720864902 ,  9211698109297098287  , 'tag5'  , 'tag5 descr'    ,'iambatman'                   , '["iambatman"]'                ,X'c11d1234586e'   ,   FALSE  ,          73600    ,      35078473        ,   8000)
;

INSERT into tagged_data_extend
(DB_ID  	,ID  	             , EXTEND_ID  	,HEIGHT  , LATEST) VALUES
(10         ,-780794814210884355 ,   1          , 2000, FALSE),
(20         ,-9128485677221760321,   2          , 3500, FALSE),
(30         ,3746857886535243786 ,   3          , 3500, FALSE),
(40         ,2083198303623116770 ,   4          , 3500, FALSE),
(50         ,808614188720864902 ,  2083198303623116770 , 8000, TRUE)
;

--Digital goods store;

INSERT into goods
(DB_ID,  	ID,  	            SELLER_ID,  	        NAME,  	                DESCRIPTION,  	            PARSED_TAGS,  	        HAS_IMAGE,  	TAGS,  	                `TIMESTAMP`, QUANTITY,  PRICE,  	    DELISTED,  	 HEIGHT,  	LATEST) values
(54,	350597963087434976	    ,200                	,'Some product'	    ,'Some product Some product'	,'["product","some","ptd","test"]'	,TRUE	,'product some ptd test'	,41814871	,2	        ,100000000000	,FALSE	    ,541839	    ,FALSE),
(55,	350597963087434976	    ,200                	,'Some product'	    ,'Some product Some product'	,'["product","some","ptd","test"]'	,TRUE	,'product some ptd test'	,41814871	,1	        ,100000000000	,FALSE	    ,541867	    ,FALSE),
(56,	350597963087434976	    ,200                	,'Some product'	    ,'Some product Some product'	,'["product","some","ptd","test"]'	,TRUE	,'product some ptd test'	,41814871	,0	        ,100000000000	,FALSE	    ,541874	    ,TRUE ),
(57,	-1443882373390424300	,9211698109297098287	,'Test product'	    ,'Test'	                        ,'["tag"]'	                ,TRUE	,'tag'	                    ,41816306	,1	        ,100000000	    ,FALSE	    ,541986	    ,FALSE),
(58,	-1443882373390424300	,9211698109297098287	,'Test product'	    ,'Test'	                        ,'["tag"]'	                ,TRUE	,'tag'	                    ,41816306	,0	        ,100000000	    ,FALSE	    ,542026	    ,TRUE ),
(59,	-2208439159357779035	,9211698109297098287	    ,'1'		        , null	                        ,'[]'                        ,TRUE	,''	                        ,38796761	,0	        ,100000000	    ,FALSE	    ,542029	    ,TRUE ),
(60,	-9001112213900824483	,3705364957971254799	,'asdffasdf'	    ,'asdf'	                        ,'["asdf"]'	                ,TRUE	,'asdf'	                    ,37965044	,222	    ,400000000	    ,FALSE	    ,542710	    ,FALSE),
(61,	-9001112213900824483	,3705364957971254799	,'asdffasdf'	    ,'asdf'	                        ,'["asdf"]'                  ,TRUE	,'asdf'	                    ,37965044	,2	        ,400000000	    ,FALSE	    ,542712	    ,FALSE),
(62,	-8243930277887618399	,9211698109297098287	,'test'	            ,'TEST'	                        ,'["sport"]'	             ,TRUE	,'sport'	                ,38188829	,21	        ,44400000000	,TRUE	    ,542717	    ,TRUE ),
(63,	-2394458469048114141	,9211698109297098287	,'fsf'	            ,'fsdfsd'	                    ,'["ff"]'	                ,TRUE	,'ff'	                    ,38176323	,1	        ,100000000000	,TRUE	    ,542719	    ,TRUE ),
(64,	8788482956389726350	    ,9211698109297098287	,'test'	            ,'test'	                    ,'["test"]'	                ,TRUE	,'test'	                    ,38189627	,2	        ,100000000	    ,FALSE	    ,542721	    ,TRUE ),
(65,	4948091426407579194	    ,9211698109297098287	,'qwe'	            ,'qwe'	                    ,'["qwe"]'	                ,TRUE	,'qwe'	                    ,38039976	,12	        ,100000000  	,FALSE	    ,542725	    ,TRUE ),
(66,	-9127861922199955586    ,9211698109297098287	,'Another product'	,'Just another produc'	    ,'["tag","batman"]'	        ,TRUE	,'tag batman'	            ,41824604	,3	        ,150000000000	,FALSE	    ,542828	    ,TRUE ),
(67,	-9001112213900824483	,3705364957971254799	,'asdffasdf'	    ,'asdf'	                        ,'["asdf"]'	                ,TRUE	,'asdf'	                    ,37965044	,2	        ,500000000	    ,FALSE	    ,542860	    ,TRUE )
;

INSERT INTO purchase
(DB_ID  	,ID  	        ,BUYER_ID  	            ,GOODS_ID  	            ,SELLER_ID  	    ,QUANTITY  	,PRICE  	,DEADLINE  	,NOTE  	,NONCE  ,`TIMESTAMP`,PENDING,GOODS  	                                                                                                                        ,GOODS_NONCE  	                                                    ,GOODS_IS_TEXT  	,REFUND_NOTE  	,REFUND_NONCE  	,HAS_FEEDBACK_NOTES  	,HAS_PUBLIC_FEEDBACKS  	,DISCOUNT  	,REFUND  	,HEIGHT  	,LATEST) VALUES
(50	,7052449049531083429	,3705364957971254799	,350597963087434976	    ,200                	,1	,100000000000	,173056826	,null	,null	,41815212	,FALSE	,X'd0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37'	                                ,X'47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,100000000	,0	        ,541915	    ,FALSE),
(51	,7938514984365421132	,3705364957971254799	,350597963087434976	    ,200                	,1	,100000000000	,173056760	,null	,null	,41815146	,FALSE	,X'cf07cf9eef82aa48108a0ed23b17178ff7d4c915d589a8adaa9775f50777e47ec3b73067b53c924c0f75c4487351f37e'	                                ,X'53da1333f562e1e33ec6294d050000a538d1ca3471e86cd49f2285aa98057236'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,300000000	,0	        ,541918	    ,FALSE),
(52	,5168710752758706151	,7821792282123976600	,-9127861922199955586	,9211698109297098287	,1	,500000000	    ,171922753	,null	,null	,41282338	,TRUE	,null	                                                                                                                                ,null	                                                                ,FALSE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,541921	    ,TRUE ),
(53	,2646157157844538473	,9211698109297098287	,-3940436337202836661	,9211698109297098287	,1	,10000000000	,167789481	,null	,null	,36547867	,FALSE	,X'0d6a37a914fbb9694d7d80fc1fdb782503e42f6b5fb36ebefa1e29fdb2c45c68f608c8d8b48b169ce7b030c8fb7ad780'	                                ,X'd35a6bfb98047c9a979fb0865e18f41242e5389e15e81be4843118c45430300b'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,541923	    ,FALSE ),
(54	,7052449049531083429	,3705364957971254799	,350597963087434976	    ,200                	,1	,100000000000	,173056826	,null	,null	,41815212	,FALSE	,X'd0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37'	                                ,X'47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97'	,TRUE	        ,null	        ,null	        ,FALSE	                ,TRUE	                 ,100000000	,0	        ,541965	    ,FALSE),
(55	,7052449049531083429	,3705364957971254799	,350597963087434976	    ,200                	,1	,100000000000	,173056826	,null	,null	,41815212	,FALSE	,X'd0e9017bfa6d02bf823e2cc5973f1ce8146ed7155ac326afb77a41d4ff2737f134a28e29be0c01357aee9660dca00a37'	                                ,X'47d7e125d33917ad2efe5cb34c754816541b3862056cae7672e8faea3fdb6c97'	,TRUE	        ,null	        ,null	        ,TRUE	                ,TRUE	                 ,100000000	,0	        ,542010	    ,TRUE ),
(56	,-3910276708092716563	,7821792282123976600	,-1443882373390424300	,9211698109297098287	,1	,100000000	    ,173058296	,null	,null	,41816681	,TRUE	,null	                                                                                                                                ,null	                                                                ,FALSE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,542026	    ,FALSE),
(57	,-1155069143692520623	,3705364957971254799	,-2208439159357779035	,9211698109297098287	,1	,100000000	    ,173058317	,null	,null	,41816705	,TRUE	,null	                                                                                                                                ,null	                                                                ,FALSE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,542029	    ,FALSE),
(58	,7938514984365421132	,3705364957971254799	,350597963087434976	    ,200                	,1	,100000000000	,173056760	,null	,null	,41815146	,FALSE	,X'cf07cf9eef82aa48108a0ed23b17178ff7d4c915d589a8adaa9775f50777e47ec3b73067b53c924c0f75c4487351f37e'	                                ,X'53da1333f562e1e33ec6294d050000a538d1ca3471e86cd49f2285aa98057236'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,300000000	,100000000	,542650	    ,TRUE ),
(59	,-1155069143692520623	,3705364957971254799	,-2208439159357779035	,9211698109297098287	,1	,100000000	    ,173058317	,null	,null	,41816705	,FALSE	,X'a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a'	,X'be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,542658	    ,FALSE),
(60	,-3910276708092716563	,7821792282123976600	,-1443882373390424300	,9211698109297098287	,1	,100000000	    ,173058296	,null	,null	,41816681	,FALSE	,X'22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68'	,X'3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,542660	    ,FALSE),
(61	,-1155069143692520623	,3705364957971254799	,-2208439159357779035	,9211698109297098287	,1	,100000000	    ,173058317	,null	,null	,41816705	,FALSE	,X'a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a'	,X'be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,1000000	,542667	    ,FALSE),
(62	,-3910276708092716563	,7821792282123976600	,-1443882373390424300	,9211698109297098287	,1	,100000000	    ,173058296	,null	,null	,41816681	,FALSE	,X'22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68'	,X'3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,5000000	,542672	    ,FALSE),
(63	,-3910276708092716563	,7821792282123976600	,-1443882373390424300	,9211698109297098287	,1	,100000000	    ,173058296	,null	,null	,41816681	,FALSE	,X'22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68'	,X'3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1'	,TRUE	        ,null	        ,null	        ,FALSE	                ,TRUE	                 ,0	        ,5000000	,542677	    ,FALSE),
(64	,-3910276708092716563	,7821792282123976600	,-1443882373390424300	,9211698109297098287	,1	,100000000	    ,173058296	,null	,null	,41816681	,FALSE	,X'22f54e5c15c5dd555c3242fdd0cedf8c9f517201bb71986b852c4cb76cae46709260329b665828af3a288f7a1c6bcabd5ec0c101586d18ac64655205a0cb1c68'	,X'3dc0bd8706d3a7644d83d8fea7189bec3e5af9a7b78698bd18a20c80f71b8cc1'	,TRUE	        ,null	        ,null	        ,TRUE	                ,TRUE	                 ,0	        ,5000000	,542682	    ,TRUE ),
(65	,-1155069143692520623	,3705364957971254799	,-2208439159357779035	,9211698109297098287	,1	,100000000	    ,173058317	,null	,null	,41816705	,FALSE	,X'a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a'	,X'be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8'	,TRUE	        ,null	        ,null	        ,TRUE	                ,FALSE	                 ,0	        ,1000000	,542688	    ,FALSE),
(66	,-1155069143692520623	,3705364957971254799	,-2208439159357779035	,9211698109297098287	,1	,100000000	    ,173058317	,null	,null	,41816705	,FALSE	,X'a83ed3a4798ac7629345f0204383f8187209a0db6167048b5634178d5c1e450e90d363068ddc65d93e3b3c4050e52c40803503388dce82d2e735f1a3fcb2086a'	,X'be123e5322ab64bbdf047c89c1010bd11d0ec262207c033325e8dda3a185f6b8'	,TRUE	        ,null	        ,null	        ,TRUE	                ,TRUE	                 ,0	        ,1000000	,542693	    ,TRUE ),
(67	,2646157157844538473	,9211698109297098287	,-3940436337202836661	,9211698109297098287	,1	,10000000000	,167789481	,null	,null	,36547867	,FALSE	,X'0d6a37a914fbb9694d7d80fc1fdb782503e42f6b5fb36ebefa1e29fdb2c45c68f608c8d8b48b169ce7b030c8fb7ad780'	                                ,X'd35a6bfb98047c9a979fb0865e18f41242e5389e15e81be4843118c45430300b'	,TRUE	        ,null	        ,null	        ,FALSE	                ,FALSE	                 ,0	        ,0	        ,542923	    ,FALSE ),
(68	,123456                 ,2000                   ,3000	                ,9211698109297098287    ,2	,10000000	    ,1000	    ,null	,null	,30000	    ,false	,null                                                                                                                                   , null	                                                                ,false	        ,null	        ,null	        ,false	                ,false	                 ,0	        ,0	        ,543000	    ,true)
;

insert into purchase_feedback
(DB_ID  	,ID  	        ,FEEDBACK_DATA  	                                                                                                                                                    ,FEEDBACK_NONCE  	                                                    ,HEIGHT ,LATEST) values
(10	,7052449049531083429	,X'5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89'	                                ,X'469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2'	,542010	,FALSE),
(20	,200                	,X'c7673f0a7f3ced8db457620b3d3d34f873cbeb1db7ffe3faea83e03b8973ef52eefcfa3651b1c6d926579e0c3eddb4e88695ad436b319b2d81ad5ce7ba0751ca'	                                ,X'1ade60abe93f5cfce908a10bb2d1b474f024779843faba4bb1f2ef06f277d3c7'	,542010	,FALSE),
(30	,7052449049531083429	,X'5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89'	                                ,X'469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2'	,542012	,FALSE),
(40	,7052449049531083429	,X'9838f3543b3f91a9ad21bde160ae1fd195b8534fe85ed1a587b27d0b8c7385dc6ad22914a2bf3693158f99026e102e8a2137ba1ecb1b81ac56f4bcfd19726f4a'	                                ,X'b45128a765486610a728f92e9cb9204d27c712a8862c3679880c0f0cdfad4605'	,542012	,FALSE),
(50	,7052449049531083429	,X'5bfe10ef4af446d18145113d2df6e35ef9a154d2db25f035ab94b243c12e498790de649e528af993eb786d10d56ea6190d081c9d379a75009a20c905912d1f89'	                                ,X'469c3ccdcc8e5dd8bdc5e1c4ff83ee5dcf4020f2eafbaf6e74bb24c4a7f65bd2'	,542014	,TRUE ),
(60	,7052449049531083429	,X'9838f3543b3f91a9ad21bde160ae1fd195b8534fe85ed1a587b27d0b8c7385dc6ad22914a2bf3693158f99026e102e8a2137ba1ecb1b81ac56f4bcfd19726f4a'	                                ,X'b45128a765486610a728f92e9cb9204d27c712a8862c3679880c0f0cdfad4605'	,542014	,TRUE ),
(70	,7052449049531083429	,X'f7f482fa35b1749ff8a095be76b5aa9260d3445990444a50bc3538b6c6c03bf46f0974fe4d02feb1d88569c15db8f380dbec193c71d8663e845110280dd23154'	                                ,X'4828b6f8e8d6854798386455c2094e90d65e9a682ef5add71965f6488b586aad'	,542014	,TRUE ),
(80	,200                	,X'c7673f0a7f3ced8db457620b3d3d34f873cbeb1db7ffe3faea83e03b8973ef52eefcfa3651b1c6d926579e0c3eddb4e88695ad436b319b2d81ad5ce7ba0751ca'	                                ,X'1ade60abe93f5cfce908a10bb2d1b474f024779843faba4bb1f2ef06f277d3c7'	,542150	,FALSE),
(90	,-3910276708092716563	,X'33c6b12600b8dba6caa21887fc4f17702d2d841395d60b7d8435b8cc8cd1e2755ed177db220f7768b4f7d26ec1abd014ad953e8f00294e7615d16186037572eabe61c17c3c71a21846d8c7b08f9be85f'	,X'955484fec03af1f97fa14e092c8d77e10c84a47bef9e5918b887f18760572429'	,542682	,TRUE ),
(100	,-1155069143692520623	,X'2ebec2f061d3b54cdfc7b8fac33762c90e37fa2bb61450726dae0425b28bbfb3dbcaf6259390abe44537f14a3ff35c08487ad65a7050082ec3ce76413aefb715f5044ff866705d1cfd4af01391f752a2'	,X'bce4d461fe18c95e6147266f8852e989a413248963828a136380f98f5fde8e44'	,542688	,FALSE),
(110	,-1155069143692520623	,X'2ebec2f061d3b54cdfc7b8fac33762c90e37fa2bb61450726dae0425b28bbfb3dbcaf6259390abe44537f14a3ff35c08487ad65a7050082ec3ce76413aefb715f5044ff866705d1cfd4af01391f752a2'	,X'bce4d461fe18c95e6147266f8852e989a413248963828a136380f98f5fde8e44'	,542690	,TRUE ),
(120	,-1155069143692520623	,X'eb07b583e5c2f52dae03872b17d31346e97b2fa1f21ad2b92f9c2a3bf349bced1780eb2858ab85035b8d437eda92a4665b3acb992bec126102a21157b785f64c'                                	,X'7f4fb46c93dc9e9c18d242fdf9be3913f38934913cac5ae682c4a59bb40a2be5'	,542690	,TRUE )
;

insert into purchase_public_feedback
(DB_ID  	,ID  	                ,PUBLIC_FEEDBACK  	        ,HEIGHT ,LATEST) values
(1          ,100                   ,'Deleted feedback'          ,541960 ,FALSE),
(2	        ,7052449049531083429	,'Feedback message'	        ,541965 ,FALSE),
(3	        ,-3910276708092716563	,'Goods dgs'	            ,542677 ,FALSE),
(4	        ,-3910276708092716563	,'Goods dgs'	            ,542679 ,TRUE ),
(5	        ,-3910276708092716563	,'Test goods feedback 2'	,542679 ,TRUE ),
(6	        ,-1155069143692520623	,'Public feedback'	        ,542693 ,FALSE),
(7	        ,-1155069143692520623	,'Public feedback'	        ,542695 ,TRUE ),
(8	        ,-1155069143692520623	,'Another Public feedback'  ,542695 ,TRUE ),
(9          ,100                    ,'Deleted feedback'         ,542695 ,FALSE),
(10	        ,7052449049531083429	,'Feedback message'	        ,542799 ,FALSE),
(11	        ,7052449049531083429	,'Public feedback 2'	    ,542799 ,FALSE),
(12	        ,7052449049531083429	,'Feedback message'	        ,542801 ,TRUE ),
(13	        ,7052449049531083429	,'Public feedback 2'	    ,542801 ,TRUE ),
(14	        ,7052449049531083429	,'Public feedback 3'	    ,542801 ,TRUE )
;

insert into tag
(DB_ID  	,TAG  	    ,IN_STOCK_COUNT  	,TOTAL_COUNT  	,HEIGHT ,LATEST ) values
(36	        ,'product'	,2	                ,4	            ,541839	,FALSE  ),
(37	        ,'some'	    ,1	                ,1	            ,541839	,FALSE  ),
(38	        ,'ptd'	    ,1	                ,1	            ,541839	,FALSE  ),
(39	        ,'deleted'	,2	                ,2	            ,541842	,FALSE  ),
(40	        ,'product'	,1	                ,4	            ,541874	,TRUE   ),
(41	        ,'some'	    ,0	                ,1	            ,541874	,TRUE   ),
(42	        ,'ptd'	    ,0	                ,1	            ,541874	,TRUE   ),
(43	        ,'tag'	    ,1	                ,1	            ,541986	,FALSE  ),
(44	        ,'deleted'	,2	                ,2	            ,541988	,FALSE  ),
(45	        ,'tag'	    ,0	                ,1	            ,542026	,FALSE  ),
(46	        ,'sport'    ,4	                ,5	            ,542717	,TRUE   ),
(47	        ,'tag'	    ,1	                ,2	            ,542828	,TRUE   ),
(48	        ,'batman'   ,1	                ,1	            ,542828	,TRUE   )
;

INSERT INTO account_control_phasing
(DB_ID, ACCOUNT_ID,                         WHITELIST,              VOTING_MODEL,   QUORUM, MIN_BALANCE, HOLDING_ID, MIN_BALANCE_MODEL, MAX_FEES,   MIN_DURATION, MAX_DURATION, HEIGHT, LATEST) VALUES
(10,    7995581942006468815,                    null,                       0,      1,      null,           null,       0,              300000000,  12,         113,            500,    true),
(20,    2728325718715804811, '[-8446656647637444484]',            0,      1,      null,           null,       0,              300000000,  12,         113,            1000,   true),
(30,    -8446384352342482748,
        '[2728325718715804811, 1344527020205736624]',             0,      1,      null,           null,       0,              300000000,  12,         113,            2000,   true),
(40,    -4013722529644937202,
    '[-8446656647637444484, 1344527020205736624, -6724281675870110558]',
                                                                            0,      1,      null,           null,       0,              300000000,  12,         113,            3000,   true)
;

INSERT into shuffling_data
(DB_ID  	,SHUFFLING_ID  	,ACCOUNT_ID  	,DATA  	                                            ,TRANSACTION_TIMESTAMP  	,HEIGHT ) VALUES
(1          ,100            ,105            ,'["ff112385a1f832bc", "ffffff", "1234567890"]'    ,150                        ,1),
(2          ,101            ,110            ,'["75849274935438"]'                                ,250                        ,5);

INSERT into `trim`
(DB_ID,     HEIGHT,   DONE ) VALUES
(1    ,      1000,    true);

INSERT into option
(NAME,     `VALUE`) VALUES
('existingKey'    ,      'existingValue'),
('existingNullKey'    ,      null),
('existingEmptyKey'    ,      '');

INSERT INTO prunable_message
(db_id         ,id                  ,sender_id               , recipient_id          ,message                                                 , encrypted_message                                                                                                                                                                                                                     ,message_is_text  , encrypted_is_text , is_compressed, block_timestamp, transaction_timestamp, height) VALUES
(1000           ,10                  ,-6004096130734886685   ,4882266200596627944    ,null                                                    ,X'b63f809f0df0b52f46225b702855c2704653e88ae96cd3dfe2c50cf2e30f747907cbb06267616241aa3aa55c2bf90457b1f275e9c96d42c7cc73cdb229a4fed055ad55245c89348c0d05c757e771996d8d597125aabb471bb25cd2d6e524f1bb9811c40f8259eff2cadf1f48df5c06f3'    ,false            , true              , true         , 128            ,  120                   , 10   ),
(1010           ,20                  ,4882266200596627944    ,-6004096130734886685   ,X'48692c20416c69636521'                                 ,X'7ac5c6571b768208df62ee48d3e9c8151abeb0f11858c66d01bdf0b8170b0741b596da28500094b25ed0bb981a41f4dfe489128c4013638d5c8eb143946b6af77b64da893560374409866b0db539ff456bbe56de583181db3ac90d67ee6f16bc0be3faa400e03ef25616b45789fde2ab'    ,true             , true              , true         , 140            ,  130                   , 12   ),
(1020           ,30                  ,-6004096130734886685   ,4882266200596627944    ,X'476f6f646279652c20426f6221'                           ,X'dd39282b7262b9369773b68a851491bbecac1f9b7a6ec078381962b129872c2df5ee11b489ef1733e78c6c54fb6fbcf992071fdb83c4e40f501b8101af76dae9c61c3726d86490c43955644a64aa004d4fa45184e37060247a9a535acdc638ac'                                    ,true             , true              , false         ,158            ,  155                   , 14   ),
(1030           ,40                  ,4882266200596627944    ,-6004096130734886685   ,X'ff3456236527fdab'                                     ,X'dae6440045c8d5bf5c561d5ed9209898654038bb375875e5a50bf0d7bb44bdcaf4c074354638aa0fe97d70d4cb00a6d62c119703b75f63d40a29190feb85ba54d9e8e433e07bfcea5923e0ff59a0e8fd3c9bdd7bcd76a08eb5bcec871c65d06f'                                    ,false            , true              , false         ,160            ,  157                   , 15   ),
(1040           ,50                  ,-6004096130734886685   ,-6004096130734886685   ,null                                                    ,X'7f39dde4494bdd8036799dc04d2e7967c3cc40af2fd3a0bd40e5113076713b9f2aa6895b6f848bfafce0fc085c991d0f0883ef75fe8b75e3bcf9308d4de27837958436fb572cf954a3dff1523908d4d09ff85558cb2bcd6ac2bba4967c4ae9c6fca25f4f8313b53b3aec308e02e3f758'    ,false            , true              , true          ,180            ,  168                   , 18   ),
(1050           ,60                  ,-5872452783836294400   ,-6004096130734886685   ,X'54657374206d65737361676520746f20416c696365'           ,null                                                                                                                                                                                                                                   ,true             , false             , false         ,185            ,  178                   , 19   ),
(1060           ,70                  ,4882266200596627944    ,4882266200596627944    ,null                                                    ,X'a8d8a1784144872e909f5c5c0eb75ea01fc45a3a0aba2f04bbe8bc29414ab1d82c617d184baf26d4dab5f6e584326eb7a69649d70906cbae8a8633c59b5357b5f19ab6a1bcc94939b33723c192c734f62886a8ad860d8bcd23398545d04776d33401adbdf1f4b72d669388ade4cc759c'    ,false            , true              , true          ,211            ,  214                   , 22   ),
(1070           ,80                  ,-5872452783836294400   ,-6004096130734886685   ,null                                                    ,X'11c48e4daac8e8582c9b83715366a0a0b4a7b7ae048d0ad115d22ae973c9c9e255fbb70f1b17168f6d15d877fa4dfd9017c8aedc9211e4576e434fb4b7102776777164f79368343936dd87f65dd58b24f61b075973c7b7c5947e5020bc835baf'                                    ,false            , true              , false         ,212            ,  225                   , 23   ),
(1080           ,90                  ,-5872452783836294400   ,4882266200596627944    ,X'f3ab4384a18c2911'                                     ,X'8de2b1bb43fc8f8ed866f551edae2f688494da7601b914fbc69f2c9c406f537845eab9a324a151d432d82a0e9d989467b1ff559a947fe8a5d0c9fe7bf0e6d0a44504273ff6b92b419abf752401b785157eb320f78e6ac13f75036a799ea47a4c'                                    ,false            , false             , false         ,232            ,  230                   , 25   ),
(1090           ,100                 ,4882266200596627944    ,-5872452783836294400   ,null                                                    ,X'a1e59a83f92fe32e2e8bd4d840adca3af792e65499ae3d87068c793daf7f7d238c9c0820c951a9280d78e492eb27fb5961a974d98f63756728cb7a22d658dabbc0c6bf192eea4f41d950cff9f51c12f03f2f853cd9ead88f3c88ebbdb1ae0423dad64b3d2c0801fc1780b41c84fc330e'    ,false            , false             , true          ,247            ,  242                   , 28   ),
(1100           ,110                 ,-6004096130734886685   ,-5872452783836294400   ,X'48656c6c6f20436875636b'                               ,null                                                                                                                                                                                                                                   ,true             , false             , false         ,259            ,  254                   , 30   )
;
INSERT INTO phasing_approval_tx
(db_id       , phasing_tx,        approved_tx,        height ) VALUES
(110         , 5                 , 120           ,    510    ),
(120         , 10                , 110           ,    525    ),
(130         , 10                , 130           ,    525    ),
(140         , 15                , 140           ,    550    )
;

INSERT INTO dex_offer
(db_id     , id              , type , account_id , offer_currency , offer_amount , pair_currency ,pair_rate ,finish_time , status , height , from_address                                ,to_address                                     , latest ) VALUES
(1000      , 1               , 0    , 100        , 0,               500000      , 1              ,1000000    ,6000        , 5      , 100    ,'0x602242c68640e754677b683e20a2740f8f95f7d3' ,'APL-K78W-Z7LR-TPJY-73HZK'                     , TRUE   ),
(1010      , 2               , 1    , 100        , 0,               200000      , 2              ,160000000  ,6500        , 3      , 110    ,'APL-K78W-Z7LR-TPJY-73HZK'                   ,'0x602242c68640e754677b683e20a2740f8f95f7d3'   , TRUE   ),
(1020      , 3               , 0    , 200        , 0,               100000      , 2              ,150000000  ,7000        , 0      , 121    ,'0x777BE94ea170AfD894Dd58e9634E442F6C5602EF' ,'APL-T69E-CTDG-8TYM-DKB5H'                     , TRUE   ),
(1030      , 4               , 1    , 100        , 0,               400000      , 1              ,1000000    ,8000        , 4      , 121    ,'APL-K78W-Z7LR-TPJY-73HZK'                   ,'0x602242c68640e754677b683e20a2740f8f95f7d3'   , TRUE   ),
(1040      , 5               , 0    , 100        , 0,               600000      , 1              ,1000000    ,11000       , 0      , 122    ,'0x602242c68640e754677b683e20a2740f8f95f7d3' ,'APL-K78W-Z7LR-TPJY-73HZK'                     , TRUE   ),
(1050      , 6               , 0    , 100        , 0,               400000      , 2              ,10000      ,13001       , 5      , 123    ,'0x602242c68640e754677b683e20a2740f8f95f7d3' ,'APL-K78W-Z7LR-TPJY-73HZK'                     , TRUE   ),
(1060      , 7               , 0    , 100        , 0,               500000      , 1              ,54000000   ,15001       , 4      , 123    ,'0x602242c68640e754677b683e20a2740f8f95f7d3' ,'APL-K78W-Z7LR-TPJY-73HZK'                     , TRUE   ),
(1070      , 8               , 0    , 200        , 0,               44000       , 2              ,430000     ,16001       , 1      , 123    ,'0x777BE94ea170AfD894Dd58e9634E442F6C5602EF' ,'APL-T69E-CTDG-8TYM-DKB5H'                     , TRUE   ),
(1080      , 9               , 1    , 100        , 0,               43000       , 1              ,7600000    ,17001       , 5      , 124    ,'APL-K78W-Z7LR-TPJY-73HZK'                   ,'0x602242c68640e754677b683e20a2740f8f95f7d3'   , TRUE   ),
(1090      , 10              , 0    , 100        , 0,               6550000     , 1              ,7400000    ,19001       , 5      , 124    ,'0x602242c68640e754677b683e20a2740f8f95f7d3' ,'APL-K78W-Z7LR-TPJY-73HZK'                     , TRUE   )
;

INSERT INTO mandatory_transaction
(db_id     , id                 , required_tx_hash                                                      , transaction_bytes) VALUES
(10        ,749837771503999228  ,X'2f23970cdc290b328e922ab0de51c288066e8579237c7b0fd45add2d064f5ff6'    ,X'09110b252703780070fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000e1f505000000000000000000000000000000000000000000000000000000000000000000000000898f755511cd0a3aec0128094bd87f996a90519e7f9c3b2b183f5d7def77c40ab18215a72f44aaa55ef304371180cfa5517554a87ffc65507dd8bd586226dea200000000000000001a51f385ecc580fe0180c13d459b696166'),
(20        ,3606021951720989487 ,null                                                                   ,X'09105c1f2703a00570fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000c2eb0b000000000000000000000000000000000000000000000000000000000000000000000000d323abad8bec5704995e40621026a93e29eba1b8726f4fbfb6f7fde06fd22a02135e46d5019536b0282beb549ab87e4f2a888dcdc615445c13d91253e950e18c00000000000000001a51f385ecc580fe0200000010a5d4e800000001102700000000000000db7028032a00307836303232343263363836343065373534363737623638336532306132373430663866393566376433180041504c2d4b3738572d5a374c522d54504a592d3733485a4b')
;

INSERT INTO dex_contract
(db_id     , id                     , offer_id               , counter_offer_id         ,     secret_hash                                                       , height    , latest,  deadline_to_reply, status, sender                , recipient             , encrypted_secret                                                                                                                      ,   transfer_tx_id                                                      ,   counter_transfer_tx_id                                              ) VALUES
(10        , -3625894990594689368   , -5227805726286506078   , -7138882269097972721     , X'F41A9D03745D78C8EFD682B4F6030FD70623E5C38AE2115D53F2C94F483AA121'    , 100       , false , 53497864          , 1     , -582612439131997299   , -582612439131997299   , X'B4F38C90AB6F36FC76013A7A69152186E2C44EF73D188A041770C253D6CCD1B88E24F37AB3C0BFD77FC74A4600C4090AEA1DC1A297A2AA3400A330CB6F670FEC'    , null                                                                  , '0x73949de85a63ed24457fc4188c8876726024a3f67fa673389a7aae47698e61bd'  ),
(20        , -7277152511870517934   , 4066034979755747272   , 6794334481055229134       , X'8E0F875179DD784241BABDC56E1380370620DB1C8AA1B7F765E2B98CD3FC2840'    , 200       , TRUE  , 53499868          , 2     , 7477442401604846627   , 7477442401604846627   , X'E670C46452E18FE2224EDF5FBA888AFFEF6060E0EFEEB10862BCFDEBFCFCF997DC37443B1FF44C79977F484E4B4E2E94404620145EBEEE5BCE7A2F609B453E13'    , '12380311258696115355'                                                , '0xe50bd6b4c62d8fb167de66c11a7a57cbcc97a2e945ddd3829d7cf0f09fda7b14'  ),
(30        , 8455581613897449491    , 5339180579805180746   , -5842203753269117069      , X'509520C8D27B08B9208B38F6AB1735C043263C18D2579A44F2210135CA92B480'    , 300       , TRUE  , 53499882          , 2     , 7477442401604846627   , -582612439131997299   , X'D6E6C72256548595C331C66D0D3FB5B1141B26E2D15946092ACB3E3E46B781F7F52148408A9F0D845333CCCAB9C822F13149EAE2AB5B963C921E4A7E97DABD7F'    , '0x8540339763b19265f394140544fe060711b1e0623860d8b99e21ffc769574f50'  , '4340657620930323843'                                                 ),
(40        , 7952648026362992483    , 6735355323156389437   , 3332621836748752862       , null                                                                  , 400       , false , 53499983          , 0     , 7477442401604846627   , -582612439131997299   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(50        , 7952648026362992483    , 6735355323156389437   , 3332621836748752862       , X'509520C8D27B08B9208B38F6AB1735C043263C18D2579A44F2210135CA92B480'    , 401       , true  , 53500038          , 1     , 7477442401604846627   , -582612439131997299   , X'D6E6C72256548595C331C66D0D3FB5B1141B26E2D15946092ACB3E3E46B781F7F52148408A9F0D845333CCCAB9C822F13149EAE2AB5B963C921E4A7E97DABD7F'    ,   null                                                                , '100'                                                                ),
(60        , 8455581613897449491    , -2195048504635381606  , 6188327480147022018       , null                                                                  , 500       , false , 53500042          , 0     , 7477442401604846627   , -582612439131997299   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(70        , -6988530272040477515   , -5716376597917953548  , 3332621836748752862       , null                                                                  , 500       , false , 53500057          , 0     , 7477442401604846627   , -582612439131997299   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(80        , 4590047955464765433    , 6876238954523300917   , -4688237877525140429      , null                                                                  , 500       , false , 53497715          , 0     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(90        , -1620743079267768652   , -5147790389386504951  , -5517784857671387693      , X'2C92403A45334780593A5D0F9E443273CD026ABE43CFAE47FA5D8E69C278C064'    , 600       , TRUE  , 53497244          , 2     , -582612439131997299   , 7477442401604846627   , X'22CD2C8D73AB0544A872963F013089017818D158FCB036040D2E411B6C80425A0EB0531252BD686892EA1AD84A54FDD7D5CE97122A903B3F3E536FEB98D01A3A'    , '0xa5c635cb164272ceb29ff055e8a6d0b2061dd886304d8b3f08800e3f4e76d3fa'  , '15853180921951477110'                                                ),
(100       , 664254800608944568     , -8286957857729261741  , 1767521844898370512       , X'AEC6592AAA4DE756E64451ABA44361327FD766F403892F4C7502659A56D27BB8'    , 700       , TRUE  , 53497244          , 2     , -582612439131997299   , 7477442401604846627   , X'D039BE0CF1318F301A8B908A3B54B1572F0DB3BE40D7CF7D5BB263AC68EAA05AD4F8300870D7BAABAB86C62199E4162C6EEFE1F36B6533D7B48951087015DFF4'    , '6606192650543722486'                                                 , '0xd0349ff2fb66d88d9c6f788b5b80b5c22aeafac349a627e1089c4305210b479d'  ),
(110       , -139104235169499924    , -2946834708490131834  , -6968465014361285240      , null                                                                  , 800       , TRUE  , 53497122          , 3     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(120       , -4808040955344135102   , -7670014354885567965  , -6968465014361285240      , null                                                                  , 800       , TRUE  , 53497141          , 3     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(130       , -4084842872841996828   , -431466151140031473   , -6968465014361285240      , null                                                                  , 800       , TRUE  , 53497194          , 3     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(140       , 1035081890238412012    , 4573417476053711227   , -6968465014361285240      , null                                                                  , 800       , TRUE  , 53497211          , 3     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  ),
(150       , -2471101731518812718   , 8603248567538608464   , -6968465014361285240      , null                                                                  , 800       , TRUE  , 53497245          , 3     , -582612439131997299   , 7477442401604846627   , null                                                                                                                                  ,   null                                                                , null                                                                  )
;




INSERT INTO dex_transaction
(db_id , hash ,                                                                                                                                 tx ,  operation , params , account , timestamp ) VALUES
(100   , X'a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26' , X'ff', 0,        100,    '0x0398E119419E0D7792c53913d3f370f9202Ae137' , 250),
(200   , X'203b36aac62037ac7c4502aa023887f7fcae843c456fde083e6a1dc70a29f3d61a73f57d79481f06e27ea279c74528e1ba6b1854d219b1e3b255729889ca5926' , X'ff', 1,        100,    '0x0398E119419E0D7792c53913d3f370f9202Ae137' , 300),
(300   , X'05ae03fd135de159cc512d0a34317d0c5270fc9d0c02ebc648828dec221272d8f20f83485bb16d0dc58acbc4a84ccc8363ef7413885936c8ee7cc943ef65cbd1' , X'ff', 0,        102,    '0x0398E119419E0D7792c53913d3f370f9202Ae137' , 400)
;
INSERT INTO user_error_message
(db_id,           address,                                      error,              operation,   details,                                                            timestamp) VALUES
(100,            '0x0398E119419E0D7792c53913d3f370f9202Ae137', 'Invalid transaction' ,'deposit',  '900'                                                             , 1000),
(200,            '0x8e96e98b32c56115614B64704bA35feFE9e8f7bC', 'Out of gas'          ,'redeem',   '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 1100),
(300,            '0x0398E119419E0D7792c53913d3f370f9202Ae137', 'Double spending'     ,'withdraw', '100'                                                              ,1200)
;
INSERT INTO account_info (DB_ID, ACCOUNT_ID, NAME, DESCRIPTION, HEIGHT, LATEST) VALUES
(3, 100, 'Madan Reddy', 'Apollo Community tokens', 2331, true),
(5, 110, 'ZT', null, 3073, true),
(6, 120, 'CALIGULA', null, 3559, true),
(7, 130, 'Adnan Celik', null, 3563, true),
(10, 140, 'Vasily', 'Front end wallet ui/ux', 26068, true),
(15, 150, 'CALIGULA shubham nitin bhabad', 'abuse brain fright always', 70858, true)
;

INSERT INTO asset
(DB_ID, ID,                     ACCOUNT_ID,  NAME,       DESCRIPTION,           QUANTITY, DECIMALS, INITIAL_QUANTITY, HEIGHT, LATEST) VALUES
(1,     -1072880289966859852,   100,        'Assets1.1', 'ThisisSecretCoin1.1',   10,        1,        10,               10,    true),
(3,     -1698552298114458330,   100,        'Assets1.2', 'ThisisSecretCoin1.2',   20,        3,        20,               30,    true),
(4,     -174530643920308495,    100,        'Assets1.3', 'ThisisSecretCoin1.3',   30,        4,        30,               40,    true),
(5,     8180990979457659735,    200,        'Assets2.1', 'ThisisSecretCoin2.1',   10,        5,        10,               50,    true),
(6,     -7411869947092956999,   200,        'Assets2.2', 'ThisisSecretCoin2.2',   20,        6,        20,               60,    true),
(8,     -2591338258392940629,   500,        'Assets3.1', 'ThisisSecretCoin3.1',   10,        8,        10,               80,    true),
(9,     1272486048634857248,    500,        'Assets3.2', 'ThisisSecretCoin3.2',   20,        9,        20,               90,    true),
(10,   -7671470345148527248,    500,        'Assets3.3', 'ThisisSecretCoin3.3',   30,        10,       30,               100,   true)
;

INSERT INTO asset_delete
(DB_ID,     ID,                         ASSET_ID,               ACCOUNT_ID,         QUANTITY, `TIMESTAMP`,  HEIGHT) VALUES
(1,         3444674909301056677,    -1072880289966859852,           100,            5,          45690782,   12),
(2,         2402544248051582903,    -1698552298114458330,           100,            10,         45690782,   32),
(3,         5373370077664349170,    -174530643920308495,            100,            10,         45712001,   42),
(4,         -780794814210884355,    -8180990979457659735,           200,            5,          45712647,   55),
(5,         -9128485677221760321,   -7411869947092956999,           200,            10,         45712817,   66),
(6,         3746857886535243786,    -2591338258392940629,           500,            5,          45712884,   81),
(7,         5471926494854938613,    1272486048634857248,            500,            12,         45712896,   94),
(8,         2083198303623116770,    -7671470345148527248,           500,            16,         45712907,   103)
;

INSERT INTO asset_dividend
(DB_ID, ID,                     ASSET_ID,               AMOUNT,     DIVIDEND_HEIGHT, TOTAL_DIVIDEND, NUM_ACCOUNTS, TIMESTAMP, HEIGHT) VALUES
(1,     7584440193513719551,    8076646017490321411,    1,          61449,           0,              0,             36559619, 61468),
(2,     -7390004979265954310,   8804302123230545017,    1000,       61449,           0,              0,             36559807, 61487),
(3,     9191632407374355191,    9065918785929852826,    1000,       61449,           0,              0,             36560092, 61516),
(4,     8033155246743541720,    9065918785929852826,    100,        61449,           0,              0,             36564916, 62007)
;

INSERT INTO asset_transfer
(DB_ID, ID,                     ASSET_ID,               SENDER_ID,              RECIPIENT_ID,           QUANTITY, `TIMESTAMP`, HEIGHT) VALUES
(1,     4942438707784864588,    8180990979457659735,    9211698109297098287,    9211698109297098287,    1,          33613556, 389),
(2,     -2439044672016971496,   -7671470345148527248,   9211698109297098287,    9211698109297098287,    1,          34842058, 20924),
(3,     4634268058494636461,    -7671470345148527248,   9211698109297098287,    9211698109297098287,    1,          34842598, 20985),
(4,     -5780986635613545285,   -7671470345148527248,   9211698109297098287,    9211698109297098287,    1,          35428343, 28704),
(5,     -8778723495575557995,   -7671470345148527248,   9211698109297098287,    9211698109297098287,    1,          35518772, 28754),
(6,     5368659037481959260,    -9067189682550466717,   -208393164898941117,    -7316102710792408068,   50,         37168105, 124462),
(7,     -6470395550179438354,   4296352812771122443,    -7316102710792408068,   -208393164898941117,    50,         37168145, 124466),
(8,     6522929850872597192,    4576453031147051032,    -208393164898941117,    -7316102710792408068,   50,         37168725, 124524),
(9,     -1727926599278750726,   1059195892779923564,    -7316102710792408068,   -208393164898941117,    50,         37168765, 124528)
;

INSERT INTO buy_offer
(DB_ID, ID,                     CURRENCY_ID,            ACCOUNT_ID,         RATE, UNIT_LIMIT, SUPPLY, EXPIRATION_HEIGHT, CREATION_HEIGHT, TRANSACTION_INDEX, TRANSACTION_HEIGHT, HEIGHT, LATEST, DELETED) VALUES
(1,     -5520700017789034517,   9017193931881541951,    -208393164898941117, 1,     999,        1,      999999999,          1383307,        0,                  1383307,         1383308, true, false),
(2,     3697010724017064611,    -4132128809614485872,   7477442401604846627, 1,     999,        1,      999999999,          1383322,        0,                  1383322,         1383324, true, false),
(3,     1046772637338198685,    -4132128809614485872,   -208393164898941117, 1,     999,        1,      999999999,          1383359,        0,                  1383359,         1383360, true, false),
(4,     -9125532320230757097,   -4649061333745309738,   7477442401604846627, 1,     999,        1,      999999999,          1383373,        0,                  1383373,         1383376, true, false),
(5,     -4337072943953941839,   4231781207816121683,    -208393164898941117, 1,     999,        1,      999999999,          1383416,        0,                  1383416,         1383418, true, false),
(6,     8038994817996483094,    -8186806310139197,      7477442401604846627, 1,     999,        1,      999999999,          1383432,        0,                  1383432,         1383435, true, false),
(7,     5036980205787824994,    -3205373316822570812,   -208393164898941117, 1,     999,        1,      999999999,          1400531,        0,                  1400531,         1400532, true, false),
(8,     7703713759586800965,    -3773624717939326451,   7477442401604846627, 1,     999,        1,      999999999,          1400544,        0,                  1400544,         1400545, true, false),
(9,     -4255505590921443908,   -3205373316822570812,   -208393164898941117, 1,     999,        1,      999999999,          1400579,        0,                  1400579,         1400580, true, false)
;

INSERT INTO sell_offer
(DB_ID, ID,                     CURRENCY_ID,            ACCOUNT_ID,         RATE, UNIT_LIMIT, SUPPLY, EXPIRATION_HEIGHT, CREATION_HEIGHT, TRANSACTION_INDEX, TRANSACTION_HEIGHT, HEIGHT, LATEST, DELETED) VALUES
(1,     -5520700017789034517,   9017193931881541951,    -208393164898941117, 1,     999,        1,      999999999,          1383307,        0,                  1383307,         1383308, true, false),
(2,     3697010724017064611,    -4132128809614485872,   7477442401604846627, 1,     999,        1,      999999999,          1383322,        0,                  1383322,         1383324, true, false),
(3,     1046772637338198685,    -4132128809614485872,   -208393164898941117, 1,     999,        1,      999999999,          1383359,        0,                  1383359,         1383360, true, false),
(4,     -9125532320230757097,   -4649061333745309738,   7477442401604846627, 1,     999,        1,      999999999,          1383373,        0,                  1383373,         1383376, true, false),
(5,     -4337072943953941839,   4231781207816121683,    -208393164898941117, 1,     999,        1,      999999999,          1383416,        0,                  1383416,         1383418, true, false),
(6,     8038994817996483094,    -8186806310139197,      7477442401604846627, 1,     999,        1,      999999999,          1383432,        0,                  1383432,         1383435, true, false),
(7,     5036980205787824994,    -3205373316822570812,   -208393164898941117, 1,     999,        1,      999999999,          1400531,        0,                  1400531,         1400532, true, false),
(8,     7703713759586800965,    -3773624717939326451,   7477442401604846627, 1,     999,        1,      999999999,          1400544,        0,                  1400544,         1400545, true, false),
(9,     -4255505590921443908,   -3205373316822570812,   -208393164898941117, 1,     999,        1,      999999999,          1400579,        0,                  1400579,         1400580, true, false)
;

INSERT INTO exchange
(DB_ID, TRANSACTION_ID,         CURRENCY_ID,            BLOCK_ID,               OFFER_ID,               SELLER_ID,              BUYER_ID,               UNITS,      RATE, TIMESTAMP, HEIGHT) VALUES
(1,     1304688235223891922,    -6000860677406393688,   -2887846745475647200,   8732480699736433017,    3494172333733565977,    3494172333733565977,    5000000000, 1,      45372444, 609481),
(2,     5294250207343561634,    -6000860677406393688,   -8152571865264379755,   8732480699736433017,    3494172333733565977,    3494172333733565977,    100000000,  1,      45535320, 612120),
(3,     -9005611557904280410,   9017193931881541951,    3255354073311876604,    -5520700017789034517,   -208393164898941117,    -208393164898941117,    100,        1,      59450358, 1383308),
(4,     -6727424768559823653,   9017193931881541951,    3255354073311876604,    -5520700017789034517,   -208393164898941117,    9211698109297098287,    200,        1,      59450358, 1383308),
(5,     7546393628201945486,    1829902366663355623,    6734429651406997110,    3697010724017064611,    7477442401604846627,    7477442401604846627,    300,        1,      59450509, 1383324),
(6,     4698684103323222902,    1829902366663355623,    6734429651406997110,    3697010724017064611,    7477442401604846627,    9211698109297098287,    400,        1,      59450509, 1383324)
;

INSERT INTO exchange_request
(DB_ID, ID,                     ACCOUNT_ID,             CURRENCY_ID,            UNITS,  RATE,   IS_BUY, TIMESTAMP, HEIGHT) VALUES
(1,     1304688235223891922,    3494172333733565977,    -6000860677406393688, 5000000000, 50,   true,   45372444,   609481),
(2,     5294250207343561634,    3494172333733565977,    -6000860677406393688, 100000000,  1,    true,   45535320,   612120),
(3,     -4059997508574268268,   3494172333733565977,    -6000860677406393688, 2500000000, 25,   false,  45535497,   612121),
(4,     -9005611557904280410,   -208393164898941117,    9017193931881541951,    1,        1,    false,  59450358,   1383308),
(5,     -6727424768559823653,   9211698109297098287,    9017193931881541951,    1,        1,    true,   59450358,   1383308),
(6,     7546393628201945486,    7477442401604846627,    1829902366663355623,    1,        1,    false,  59450509,   1383324),
(7,     4698684103323222902,    9211698109297098287,    1829902366663355623,    1,        1,    true,   59450509,   1383324)
;