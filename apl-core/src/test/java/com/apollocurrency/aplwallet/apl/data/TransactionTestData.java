package com.apollocurrency.aplwallet.apl.data;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_0;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_1;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_2;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_3;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class TransactionTestData {

    public final long DB_ID_0  = 150;
    public final long DB_ID_1  = 175;
    public final long DB_ID_2  = 200;
    public final long DB_ID_3  = 500;
    public final long DB_ID_4  = 1000;
    public final long DB_ID_5  = 1500;
    public final long DB_ID_6  = 2000;
    public final long DB_ID_7  = 2500;
    public final long DB_ID_8  = 3000;
    public final long DB_ID_9  = 3500;
    public final long DB_ID_10 = 4000;
    public final long DB_ID_11 = 4500;
    public final long DB_ID_12 = 5000;

    public final Transaction TRANSACTION_0 = buildTransaction(3444674909301056677L,  (short) 1440, null,                 (short) 0, 0L,                  2500000000000L, "a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", BLOCK_0_HEIGHT ,  BLOCK_0_ID , "375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc", 35073712, (byte)5, (byte)0, 9211698109297098287L, BLOCK_0_TIMESTAMP, "6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", false, (byte)1, false, false, false, 14399, -5416619518547901377L, false, false, false, false, "01056673646673035145520500667364667301ae150000000000000000000000000000ae150000000000000000000000000000000000000000000001");
    public final Transaction TRANSACTION_1 = buildTransaction(2402544248051582903L,  (short) 1440, null,                 (short) 0, 0L,                  1000000000000L, "b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d", BLOCK_0_HEIGHT ,  BLOCK_0_ID , "fc6f11f396aa20717c9191a1fb25fab0681512cc976c935db1563898aabad90ffc6ced28e1b8b3383d5abb55928bbb122a674dc066ab8b0cc585b9b4cdbd8fac", 35075179, (byte)2, (byte)0, 9211698109297098287L, BLOCK_0_TIMESTAMP, "6500000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", false, (byte)1, false, false, false, 14405, -2297016555338476945L, false, false, false, false, "01074d5941535345540b00666466736b64666c616c73102700000000000002");
    public final Transaction TRANSACTION_2 = buildTransaction(5373370077664349170L,  (short) 1440, 457571885748888948L,  (short) 0, 100000000000000000L, 100000000,      "f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c", BLOCK_1_HEIGHT ,  BLOCK_1_ID , "8afd3a91d0e3011e505e0353b1f7089c0d401672f8ed5d0ddc2107e0b130aa0bdd17f03b2d75eed8fcc645cda88b5c82ac1b621c142abad9b1bb95df517aa70c", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_1_TIMESTAMP, "b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_3 = buildTransaction(-780794814210884355L,  (short) 1440, 6110033502865709882L, (short) 1, 100000000000000000L, 100000000,      "fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558", BLOCK_2_HEIGHT ,  BLOCK_2_ID , "240b0a1ee9f63f5c3cb914b42584da1388b9d048a981f1651ac85dd12f12660c29782100c03cbe8491bdc831aa27f6fd3a546345b3da7860c56e6ba431550517", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_2_TIMESTAMP, "f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_4 = buildTransaction(-9128485677221760321L, (short) 1440, -603599418476309001L, (short) 0, 100000000000000000L, 100000000,      "bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1", BLOCK_5_HEIGHT ,  BLOCK_5_ID , "75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_5_TIMESTAMP, "fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_5 = buildTransaction(3746857886535243786L,  (short) 1440, -693728062313138401L, (short) 1, 100000000000000000L, 100000000,      "0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3", BLOCK_5_HEIGHT ,  BLOCK_5_ID , "73a84f612f5957453b502ae8aeaa31bc2add030b1b9182624c66eb94d6377000462286b22ca7fcd6e13987292858b02b0b14ac4539b97df4bd3b14303797f11b", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_5_TIMESTAMP, "bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_6 = buildTransaction(5471926494854938613L,  (short) 1440, -3934231941937607328L,(short) 2, 100000000000000000L, 100000000,      "f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691", BLOCK_5_HEIGHT ,  BLOCK_5_ID , "98f5fc631ea607b47bf7888eb3253e0e696f5fd4bf26d6c698a9c69e1078ab0ff7afc6e76c5b1043f6ff00ecea2fed75c83dcbac754c195f29a61a6632010a39", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_5_TIMESTAMP, "0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_7 = buildTransaction(2083198303623116770L,  (short) 1440, -1017037002638468431L,(short) 0, 100000000000000000L, 100000000,      "e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f", BLOCK_7_HEIGHT ,  BLOCK_7_ID , "d24811bc4be2c7031196fd220639f1885c8e15c96e7672146c88c2eea25d8a0cd4e93b8e2324e2522e3aff14faa1ef811fc43a971fdbdb71f7ac0b5614e706cb", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_7_TIMESTAMP, "f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_8 = buildTransaction(808614188720864902L,   (short) 1440, -5803127966835594607L,(short) 1, 100000000000000000L, 100000000,      "863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6", BLOCK_7_HEIGHT ,  BLOCK_7_ID , "38484c6128b2707a81ea6f0c9f19663dbcd54358e644d56cfa2b33635f2d570f7b91c41820f8d1923e0afca5cb0e5785c76c2fd859e354c876a9640a75882aa2", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_7_TIMESTAMP, "e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_9 = buildTransaction(-2262365651675616510L, (short) 1440, 2569665864951373924L, (short) 0, 100000000000000000L, 100000000,      "026bd4236d769ae022df97e248c6292aef1f403f5d5dcb74d787255344cf58e5", BLOCK_10_HEIGHT,  BLOCK_10_ID, "1a3ecfc672df4ae91b1bcf319cee962426cd3f65fac340a0e01ac27367646904fa8ccf22f0b0c93f84d00584fa3f7f5bd03933e08b3aa1295a9ebdd09a0c1654", 35078473, (byte)0, (byte)0, 9211698109297098287L, BLOCK_10_TIMESTAMP, "863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6", false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_10 = buildTransaction(9145605905642517648L, (short) 1440, 2230095012677269409L, (short) 1, 100000000000000000L, 100000000,      "9074899d1db8eb7e807f0d841973fdc8a84ab2742a4fb03d47b620f5e920e5fe", BLOCK_10_HEIGHT,  BLOCK_10_ID, "6ae95b4165ef53b335ac576a72d20d24464f57bd49dbdd76dd22f519caff3d0457d97769ae76d8496906e4f1ab5f7db30db73daea5db889d80e1ac0bd4b05257", 35078473, (byte)0, (byte)0, -8315839810807014152L, BLOCK_10_TIMESTAMP, null,                                                               false, (byte)1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
    public final Transaction TRANSACTION_11 = buildTransaction(-1536976186224925700L,(short) 1440, null,                 (short) 2, 0L,                  100000000,      "fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f", BLOCK_10_HEIGHT,  BLOCK_10_ID, "61a224ae2d8198bfcee91c83e449d6325a2caa974f6a477ab59d0072b9b7e50793575534ab29c7be7d3dbef46f5e9e206d0bf5801bebf06847a28aa16c6419a1", 16758888, (byte)8, (byte)1, 9211698109297098287L, BLOCK_10_TIMESTAMP, "830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088",false, (byte)1, false, false, false, 103874, 1281949812948953897L, false, false, false, false, "01054c494e5558035838360002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
    public final Transaction TRANSACTION_12 = buildTransaction(-4081443370478530685L,(short) 1440, null,                 (short) 3, 0L,                  100000000,      "830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088", BLOCK_10_HEIGHT,  BLOCK_10_ID, "551f99bc4eceaae7c7007ac077ed163f4d95f8acc0119e38b726b5c8b494cf09c5059292de17efbc4ec14848e3944ecd0a5d0ca2591177266e04d426ce25a1c1", 16763004, (byte)8, (byte)0, 9211698109297098287L, BLOCK_10_TIMESTAMP, null,                                                               false, (byte)1, false, false, false, 103950, 3234042379296483074L, false, false, false, false, "01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
    public final Transaction NOT_SAVED_TRANSACTION = buildTransaction(-5176698353372716962L,	(short) 1440,	5338910736239317247L,	(short) 204,	900000000L,	100000000L,	"5ea0de6146ac28b8b64d4f7f1ccbd1c7b2e43397221ef7ed3fa10c4ec0581d43", BLOCK_10_HEIGHT,BLOCK_10_ID,  "afb31f67c1101c2eae312da60b7087b122cada3c929ac032e3b3a6079f61a905bb6787128a2c37bd76bdc8640d82762589fbe31cc145f11ba0359bd55800501d",	33614539,(byte) 	0	,(byte) 0,	-7834552789888196284L,	33614621,	null,	false,	(byte)1,	false,	false,	false,	0,	-107868771406622438L,	false,	false,false,	false, null);
    public final ReferencedTransaction REFERENCED_SHARD_TRANSACTION_0 = new ReferencedTransaction(1L, TRANSACTION_INDEX_0.getTransactionId(),TRANSACTION_INDEX_1.getTransactionId());
    public final ReferencedTransaction REFERENCED_SHARD_TRANSACTION_1 = new ReferencedTransaction(2L, TRANSACTION_INDEX_1.getTransactionId(), TRANSACTION_INDEX_2.getTransactionId());
    public final ReferencedTransaction REFERENCED_SHARD_TRANSACTION_2 = new ReferencedTransaction(3L, TRANSACTION_INDEX_2.getTransactionId(), TRANSACTION_INDEX_3.getTransactionId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_0 = new ReferencedTransaction(1L, TRANSACTION_0.getId(), TRANSACTION_INDEX_0.getTransactionId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_1 = new ReferencedTransaction(2L, TRANSACTION_1.getId(), TRANSACTION_INDEX_1.getTransactionId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_2 = new ReferencedTransaction(3L, TRANSACTION_2.getId(), TRANSACTION_1.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_3 = new ReferencedTransaction(4L, TRANSACTION_3.getId(), TRANSACTION_2.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_4 = new ReferencedTransaction(5L, TRANSACTION_4.getId(), TRANSACTION_3.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_5 = new ReferencedTransaction(6L, TRANSACTION_5.getId(), TRANSACTION_4.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_6 = new ReferencedTransaction(7L, TRANSACTION_6.getId(), TRANSACTION_5.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_7 = new ReferencedTransaction(8L, TRANSACTION_7.getId(), TRANSACTION_6.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_8 = new ReferencedTransaction(9L, TRANSACTION_8.getId(), TRANSACTION_7.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_9 = new ReferencedTransaction(10L,TRANSACTION_9.getId(), TRANSACTION_8.getId());
    public final ReferencedTransaction REFERENCED_TRANSACTION_10 = new ReferencedTransaction(11L,TRANSACTION_11.getId(), TRANSACTION_8.getId());
    public final ReferencedTransaction NOT_SAVED_REFERENCED_SHARD_TRANSACTION = new ReferencedTransaction(11L, TRANSACTION_10.getId(), TRANSACTION_11.getId());
    public final List<ReferencedTransaction> REFERENCED_TRANSACTIONS = Arrays.asList(REFERENCED_TRANSACTION_0, REFERENCED_TRANSACTION_1, REFERENCED_TRANSACTION_2, REFERENCED_TRANSACTION_3,
            REFERENCED_TRANSACTION_4, REFERENCED_TRANSACTION_5, REFERENCED_TRANSACTION_6, REFERENCED_TRANSACTION_7,REFERENCED_TRANSACTION_8, REFERENCED_TRANSACTION_9, REFERENCED_TRANSACTION_10, REFERENCED_SHARD_TRANSACTION_0, REFERENCED_SHARD_TRANSACTION_1, REFERENCED_SHARD_TRANSACTION_2);

    public TransactionTestData() {
    }
    
    private static Transaction buildTransaction(long id, short deadline, Long recipientId, short index, long amount, long fee, String fullHash, int height, long blockId, String signature, int timestamp, byte type, byte subtype, long senderId, int blockTimestamp, String referencedTransactionFullhash, boolean phased, byte version, boolean hasMessage, boolean hasEncryptedMessage, boolean hasAnnouncedPublicKey, int ecBlockHeight, long ecBlockId, boolean hasEncrypttoselfMessage, boolean hasPrunableMessage, boolean hasPrunableEncryptedMessage, boolean hasPrunableAttachment, String attachment) {
        ByteBuffer buffer = null;
        if (attachment != null) {
            buffer = ByteBuffer.wrap(Convert.parseHexString(attachment));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
        try {
            Transaction.Builder builder = new TransactionImpl.BuilderImpl(version, null,
                    amount, fee, deadline, transactionType.parseAttachment(buffer), timestamp)
                    .referencedTransactionFullHash(referencedTransactionFullhash)
                    .signature(Convert.parseHexString(signature))
                    .blockId(blockId)
                    .height(height)
                    .id(id)
                    .senderId(senderId)
                    .blockTimestamp(blockTimestamp)
                    .fullHash(Convert.parseHexString(fullHash))
                    .ecBlockHeight(ecBlockHeight)
                    .ecBlockId(ecBlockId)
                    .index(index);
            if (transactionType.canHaveRecipient()) {
                if (recipientId != null) {
                    builder.recipientId(recipientId);
                }
            }
            if (hasMessage) {
                builder.appendix(new MessageAppendix(buffer));
            }
            if (hasEncryptedMessage) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            if (hasAnnouncedPublicKey) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            if (hasEncrypttoselfMessage) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            if (phased) {
                builder.appendix(new PhasingAppendix(buffer));
            }
            if (hasPrunableMessage) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            if (hasPrunableEncryptedMessage) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }
            return builder.build();
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e);
        }

    }
}
