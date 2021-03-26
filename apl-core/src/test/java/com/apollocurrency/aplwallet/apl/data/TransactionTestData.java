package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.types.cc.CCAssetIssuanceTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.dgs.ListingTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.AliasAssignmentTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.ArbitraryMessageTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MSCurrencyDeletionTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MSCurrencyIssuanceTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.OrdinaryPaymentTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.PrivatePaymentTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.smc.SmcCallMethodTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.smc.SmcPublishContractTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.CriticalUpdateTransactiionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.ImportantUpdateTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import lombok.Getter;
import lombok.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_13_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_13_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_13_TIMESTAMP;
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
import static org.mockito.Mockito.mock;

public class TransactionTestData {

    public final long DB_ID_0 = 150;
    public final long DB_ID_1 = 175;
    public final long DB_ID_2 = 200;
    public final long DB_ID_3 = 500;
    public final long DB_ID_4 = 1000;
    public final long DB_ID_5 = 1500;
    public final long DB_ID_6 = 2000;
    public final long DB_ID_7 = 2500;
    public final long DB_ID_8 = 3000;
    public final long DB_ID_9 = 3500;
    public final long DB_ID_10 = 4000;
    public final long DB_ID_11 = 4500;
    public final long DB_ID_12 = 5000;
    public final long DB_ID_13 = 6000;
    public final long DB_ID_14 = 7000;

    public Transaction TRANSACTION_0;
    public Transaction TRANSACTION_1;
    public Transaction TRANSACTION_2;
    public Transaction TRANSACTION_3;
    public Transaction TRANSACTION_4;
    public Transaction TRANSACTION_5;
    public Transaction TRANSACTION_6;
    public Transaction TRANSACTION_7;
    public Transaction TRANSACTION_8;
    public Transaction TRANSACTION_9;
    public Transaction TRANSACTION_10;
    public Transaction TRANSACTION_11;
    public Transaction TRANSACTION_12;
    public Transaction TRANSACTION_13;
    public Transaction TRANSACTION_14;

    public Transaction NOT_SAVED_TRANSACTION;

    public Transaction NEW_TRANSACTION_0;
    public Transaction NEW_TRANSACTION_1;

    public Transaction TRANSACTION_V2_1;
    public Transaction TRANSACTION_V2_2;

    public ReferencedTransaction REFERENCED_TRANSACTION_0;
    public ReferencedTransaction REFERENCED_TRANSACTION_1;
    public ReferencedTransaction REFERENCED_TRANSACTION_2;
    public ReferencedTransaction REFERENCED_TRANSACTION_3;
    public ReferencedTransaction REFERENCED_TRANSACTION_4;
    public ReferencedTransaction REFERENCED_TRANSACTION_5;
    public ReferencedTransaction REFERENCED_TRANSACTION_6;
    public ReferencedTransaction REFERENCED_TRANSACTION_7;
    public ReferencedTransaction REFERENCED_TRANSACTION_8;
    public ReferencedTransaction REFERENCED_TRANSACTION_9;
    public ReferencedTransaction REFERENCED_TRANSACTION_10;
    public ReferencedTransaction REFERENCED_TRANSACTION_11;
    public ReferencedTransaction REFERENCED_TRANSACTION_12;
    public ReferencedTransaction REFERENCED_TRANSACTION_13;
    public ReferencedTransaction NOT_SAVED_REFERENCED_SHARD_TRANSACTION;
    public List<ReferencedTransaction> REFERENCED_TRANSACTIONS;
    private final TransactionTypeFactory transactionTypeFactory;

    @Getter
    private BlockchainConfig blockchainConfig;

    public TransactionTypeFactory getTransactionTypeFactory() {
        return transactionTypeFactory;
    }

    public TransactionTestData() {
        blockchainConfig = mock(BlockchainConfig.class);
        AccountService accountService = mock(AccountService.class);
        AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
        CurrencyService currencyService = mock(CurrencyService.class);
        AccountCurrencyService accountCurrencyService = mock(AccountCurrencyService.class);
        AccountAssetService accountAssetService = mock(AccountAssetService.class);
        AssetService assetService = mock(AssetService.class);
        AliasService aliasService = mock(AliasService.class);
        DGSService dgsService = mock(DGSService.class);
        PrunableLoadingService prunableLoadingService = mock(PrunableLoadingService.class);
        ContractService contractService = mock(ContractService.class);
        SMCMachineFactory smcMachineFactory = mock(SMCMachineFactory.class);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(
            new OrdinaryPaymentTransactionType(blockchainConfig, accountService),
            new PrivatePaymentTransactionType(blockchainConfig, accountService),
            new MSCurrencyIssuanceTransactionType(blockchainConfig, accountService, currencyService, accountCurrencyService),
            new CCAssetIssuanceTransactionType(blockchainConfig, accountService, assetService, accountAssetService),
            new AliasAssignmentTransactionType(blockchainConfig, accountService, aliasService),
            new ArbitraryMessageTransactionType(blockchainConfig, accountService),
            new CriticalUpdateTransactiionType(blockchainConfig, accountService),
            new ImportantUpdateTransactionType(blockchainConfig, accountService),
            new ListingTransactionType(blockchainConfig, accountService, dgsService, prunableLoadingService),
            new SmcPublishContractTransactionType(blockchainConfig, accountService, contractService, smcMachineFactory),
            new SmcCallMethodTransactionType(blockchainConfig, accountService, contractService, smcMachineFactory),
            new MSCurrencyDeletionTransactionType(blockchainConfig, accountService, currencyService)
        ));
        initTransactions();
    }

    private void initTransactions() {
        TRANSACTION_0 = buildTransaction(3444674909301056677L, BLOCK_0_HEIGHT, BLOCK_0_ID, BLOCK_0_TIMESTAMP, (short) 1440, null, (short) 0, 0L, 2500000000000L, "a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", "375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc", 35073712, (byte) 5, (byte) 0, 9211698109297098287L, null, "6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", false, (byte) 1, false, false, false, 14399, -5416619518547901377L, false, false, false, false, "01056673646673035145520500667364667301ae150000000000000000000000000000ae150000000000000000000000000000000000000000000001");
        TRANSACTION_1 = buildTransaction(2402544248051582903L, BLOCK_0_HEIGHT, BLOCK_0_ID, BLOCK_0_TIMESTAMP, (short) 1440, null, (short) 1, 0L, 1000000000000L, "b7c745ae438d5721b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d", "fc6f11f396aa20717c9191a1fb25fab0681512cc976c935db1563898aabad90ffc6ced28e1b8b3383d5abb55928bbb122a674dc066ab8b0cc585b9b4cdbd8fac", 35075179, (byte) 2, (byte) 0, 9211698109297098287L, null, "6500000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e", false, (byte) 1, false, false, false, 14405, -2297016555338476945L, false, false, false, false, "01074d5941535345540b00666466736b64666c616c73102700000000000002");
        TRANSACTION_2 = buildTransaction(5373370077664349170L, BLOCK_1_HEIGHT, BLOCK_1_ID, BLOCK_1_TIMESTAMP, (short) 1440, 457571885748888948L, (short) 0, 100000000000000000L, 100000000, "f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c", "8afd3a91d0e3011e505e0353b1f7089c0d401672f8ed5d0ddc2107e0b130aa0bdd17f03b2d75eed8fcc645cda88b5c82ac1b621c142abad9b1bb95df517aa70c", 35078473, (byte) 8, (byte) 0, 9211698109297098287L, null, "b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, "01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
        TRANSACTION_3 = buildTransaction(-780794814210884355L, BLOCK_2_HEIGHT, BLOCK_2_ID, BLOCK_2_TIMESTAMP, (short) 1440, 6110033502865709882L, (short) 1, 100000000000000000L, 100000000, "fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558", "240b0a1ee9f63f5c3cb914b42584da1388b9d048a981f1651ac85dd12f12660c29782100c03cbe8491bdc831aa27f6fd3a546345b3da7860c56e6ba431550517", 35078473, (byte) 8, (byte) 0, 9211698109297098287L, null, "f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, "01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
        TRANSACTION_4 = buildTransaction(-9128485677221760321L, BLOCK_5_HEIGHT, BLOCK_5_ID, BLOCK_5_TIMESTAMP, (short) 1440, -603599418476309001L, (short) 0, 100000000000000000L, 100000000, "bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1", "75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f", 35078473, (byte) 0, (byte) 0, 9211698109297098287L, null, "fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_5 = buildTransaction(3746857886535243786L, BLOCK_5_HEIGHT, BLOCK_5_ID, BLOCK_5_TIMESTAMP, (short) 1440, -693728062313138401L, (short) 1, 100000000000000000L, 100000000, "0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3", "73a84f612f5957453b502ae8aeaa31bc2add030b1b9182624c66eb94d6377000462286b22ca7fcd6e13987292858b02b0b14ac4539b97df4bd3b14303797f11b", 35078473, (byte) 0, (byte) 0, 9211698109297098287L, null, "bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_6 = buildTransaction(5471926494854938613L, BLOCK_5_HEIGHT, BLOCK_5_ID, BLOCK_5_TIMESTAMP, (short) 1440, -3934231941937607328L, (short) 2, 100000000000000000L, 100000000, "f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691", "98f5fc631ea607b47bf7888eb3253e0e696f5fd4bf26d6c698a9c69e1078ab0ff7afc6e76c5b1043f6ff00ecea2fed75c83dcbac754c195f29a61a6632010a39", 35078473, (byte) 0, (byte) 0, 9211698109297098287L, null, "0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_7 = buildTransaction(2083198303623116770L, BLOCK_7_HEIGHT, BLOCK_7_ID, BLOCK_7_TIMESTAMP, (short) 1440, -1017037002638468431L, (short) 0, 100000000000000000L, 100000000, "e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f", "d24811bc4be2c7031196fd220639f1885c8e15c96e7672146c88c2eea25d8a0cd4e93b8e2324e2522e3aff14faa1ef811fc43a971fdbdb71f7ac0b5614e706cb", 35078473, (byte) 8, (byte) 0, 9211698109297098287L, null, "f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, "01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
        TRANSACTION_8 = buildTransaction(808614188720864902L, BLOCK_7_HEIGHT, BLOCK_7_ID, BLOCK_7_TIMESTAMP, (short) 1440, -5803127966835594607L, (short) 1, 100000000000000000L, 100000000, "863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6", "38484c6128b2707a81ea6f0c9f19663dbcd54358e644d56cfa2b33635f2d570f7b91c41820f8d1923e0afca5cb0e5785c76c2fd859e354c876a9640a75882aa2", 35078473, (byte) 0, (byte) 0, 9211698109297098287L, null, "e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_9 = buildTransaction(-2262365651675616510L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, 2569665864951373924L, (short) 0, 100000000000000000L, 100000000, "026bd4236d769ae022df97e248c6292aef1f403f5d5dcb74d787255344cf58e5", "1a3ecfc672df4ae91b1bcf319cee962426cd3f65fac340a0e01ac27367646904fa8ccf22f0b0c93f84d00584fa3f7f5bd03933e08b3aa1295a9ebdd09a0c1654", 35078473, (byte) 0, (byte) 0, 9211698109297098287L, null, "863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6", false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_10 = buildTransaction(9145605905642517648L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, 2230095012677269409L, (short) 1, 100000000000000000L, 100000000, "9074899d1db8eb7e807f0d841973fdc8a84ab2742a4fb03d47b620f5e920e5fe", "6ae95b4165ef53b335ac576a72d20d24464f57bd49dbdd76dd22f519caff3d0457d97769ae76d8496906e4f1ab5f7db30db73daea5db889d80e1ac0bd4b05257", 35078474, (byte) 0, (byte) 0, -8315839810807014152L, null, null, false, (byte) 1, false, false, false, 14734, 2621055931824266697L, false, false, false, false, null);
        TRANSACTION_11 = buildTransaction(-1536976186224925700L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, null, (short) 2, 0L, 100000000, "fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f", "61a224ae2d8198bfcee91c83e449d6325a2caa974f6a477ab59d0072b9b7e50793575534ab29c7be7d3dbef46f5e9e206d0bf5801bebf06847a28aa16c6419a1", 36758888, (byte) 8, (byte) 0, 9211698109297098287L, null, "830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088", false, (byte) 1, false, false, false, 103874, 1281949812948953897L, false, false, false, false, "01054c494e5558035838360002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
        TRANSACTION_12 = buildTransaction(-4081443370478530685L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, null, (short) 3, 0L, 100000000, "830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088", "551f99bc4eceaae7c7007ac077ed163f4d95f8acc0119e38b726b5c8b494cf09c5059292de17efbc4ec14848e3944ecd0a5d0ca2591177266e04d426ce25a1c1", 36763004, (byte) 8, (byte) 0, 9211698109297098287L, null, null, false, (byte) 1, false, false, false, 103950, 3234042379296483074L, false, false, false, false, "01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4");
        TRANSACTION_13 = buildTransaction(4851834545659781120L, BLOCK_12_HEIGHT, BLOCK_12_ID, BLOCK_12_TIMESTAMP, (short) 1440, 7477442401604846627L, (short) 0, 12000000000L, 23_000_000_000L, "0020052bd02d5543c4408aed90d98e636fdb21447cbed0c1f1e2db3134e37fbf", "7ace0ea75778aebb8363e141da74b4efce571dc73c728de7f3bcd6126fe3ab04fb1b8e3170e6fe4e458f9fd40f8d10ef7bc8caa839ae9c28a2276f02ddccd2ff", 41571172, (byte) 0, (byte) 0, 9211698109297098287L, "bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37", null, true, (byte) 1, true, false, false, 516746, 5629144656878115682L, true, false, true, false, "010c00008054657374206d65737361676501400000808bb31f0eb60af644d69bad77c5158ceac89bb3b02856542f334de903be92ad354d11f1f5eb876d3e558c40513c813248a879751d03d6446d6c562e04306573f6adcb4a9238585b1f9f1df4c124055da5ba78d76521eb2ace178f552d064a2cf802a83108000232000000000000000a0000000000000002dc3fd47da87a5620983fe492a3968c6c93931ffe397ff94202000000ffffffff019fec636832fa9108934bac4902b7bd9213f4c0f073625dcdc9a2c511cc715fdc");
        TRANSACTION_14 = buildTransaction(9175410632340250178L, BLOCK_13_HEIGHT, BLOCK_13_ID, BLOCK_13_TIMESTAMP, (short) 1440, null, (short) 0, 0, 1000000000, "429efb505b9b557f5d2a1d6d506cf75de6c3692ca1a21217ae6160c7658c7312", "7ecae5825a24dedc42dd11e2239ced7ad797c6d6c9aedc3d3275204630b7e20832f9543d1063787ea1f32ab0993ea733aa46a52664755d9e54f211cdc3c5c5fd", 41974329, (byte) 3, (byte) 0, 3705364957971254799L, "39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152", null, false, (byte) 1, false, false, false, 552605, 4407210215527895706L, false, true, false, false, "010c00546573742070726f647563741500546573742070726f6475637420666f722073616c650c007461672074657374646174610200000000e40b540200000001b9dd15475e2f8da755f1b63933051dede676b223c86e70f54c7182b976d2f86d");
        NEW_TRANSACTION_0 = buildTransaction(1854970739572493540L, 570432, -2393094257044436049L, 43031981, (short) 1440, null, (short) 1, 0, 2500000000L, "e44cf6400f2ebe19f0270ad9dd608eaa05fd67d9210f96431ec54e928f1d67fc", "5de61cc2d4c6f2f9356e050b9f071e70f23ffa939ac85b96eba2af8d6197d20323fb55762544f6a06befeb940c39aaca9aaac9b410853419e11aee96741cf6d0", 43029026, (byte) 1, (byte) 1, 7821792282123976600L, "129749b75f13a861a24619f5994dc24ed63df7bd49ce6331f68517a8c611ef1d", null, false, (byte) 1, false, false, false, 569669, 3522593573338414470L, false, false, false, false, "010e616c696173666f7264656c6574650e007777772e64656c6574652e636f6d");
        NEW_TRANSACTION_1 = buildTransaction(-3727347706778021725L, 570432, -2393094257044436049L, 43031981, (short) 1440, null, (short) 0, 0, 400000000, "a310214f02cd45cc50ae427baba04644d3dc631880010ce268470d1f296321a1", "a21327a4cbd416bf3760686944d609e9219629325e04dba7cc412d7488ef240689342eed703c203cc4a17ad6d7251af8b634932affdef603e14ce988118a7b68", 43021658, (byte) 3, (byte) 0, 7821792282123976600L, "129749b75f13a861a24619f5994dc24ed63df7bd49ce6331f68517a8c611ef1d", null, false, (byte) 1, false, false, false, 569541, 6726658595929558334L, false, false, false, false, "010e0050726f64756374466f7253616c651f006465736372697074696f6e206f662070726f6475637420666f722053616c6504006175746f0a0000000a00000000000000");
        NOT_SAVED_TRANSACTION = buildTransaction(-5176698353372716962L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, 5338910736239317247L, (short) 204, 900000000L, 100000000L, "5ea0de6146ac28b8b64d4f7f1ccbd1c7b2e43397221ef7ed3fa10c4ec0581d43", "afb31f67c1101c2eae312da60b7087b122cada3c929ac032e3b3a6079f61a905bb6787128a2c37bd76bdc8640d82762589fbe31cc145f11ba0359bd55800501d", 33614539, (byte) 0, (byte) 0, -7834552789888196284L, "7f7aee80a5f9b4460945ef564099c6774fd92f031e4773d9da467924d274004c", null, false, (byte) 1, false, false, false, 0, -107868771406622438L, false, false, false, false, null);
        TRANSACTION_V2_1 = buildTransaction(-5558468054444673854L, BLOCK_10_HEIGHT, BLOCK_10_ID, BLOCK_10_TIMESTAMP, (short) 1440, 7649455923293388179L, (short) 0, 35500000000L, 100000000, "5ea0de6146ac28b8b64d4f7f1ccbd1c7b2e43397221ef7ed3fa10c4ec0581d43", "4d53494700000000020039dc2e813bb45ff0c3eafbed56c02be9fe3da9691cb0ffd8b897ab5dcb7e9b70d44ace089e710908acc3be46c5c6d34dc95813c8cb4c2f68ad0ab19b10def07fdf16c9d787ddb951fd14af33957458d911f944ebdb2780db5c4fa87af3b75ff63a08016beaaf761a820cff3fe723d3048563db5790f18663f98282c5ba3b1f9bc0cb864c054f0d7b9ce02e769134f8c4", 86652701, (byte) 0, (byte) 0, 6935955567226948703L, "fd14af33957458d99c8abf3d31839a58c21fa0689f99526ef48ad70ae271a46d", null, false, (byte) 2, false, false, false, 25606, 4105716013512048697L, false, false, false, false, null);
        REFERENCED_TRANSACTION_0 = new ReferencedTransaction(10L, TRANSACTION_INDEX_0.getTransactionId(), TRANSACTION_INDEX_1.getTransactionId(), 100);
        REFERENCED_TRANSACTION_1 = new ReferencedTransaction(20L, TRANSACTION_INDEX_1.getTransactionId(), TRANSACTION_INDEX_2.getTransactionId(), 200);
        REFERENCED_TRANSACTION_2 = new ReferencedTransaction(30L, TRANSACTION_INDEX_2.getTransactionId(), TRANSACTION_INDEX_3.getTransactionId(), 300);
        REFERENCED_TRANSACTION_3 = new ReferencedTransaction(40L, TRANSACTION_0.getId(), TRANSACTION_INDEX_0.getTransactionId(), TRANSACTION_0.getHeight());
        REFERENCED_TRANSACTION_4 = new ReferencedTransaction(50L, TRANSACTION_1.getId(), TRANSACTION_INDEX_1.getTransactionId(), TRANSACTION_1.getHeight());
        REFERENCED_TRANSACTION_5 = new ReferencedTransaction(60L, TRANSACTION_2.getId(), TRANSACTION_1.getId(), TRANSACTION_2.getHeight());
        REFERENCED_TRANSACTION_6 = new ReferencedTransaction(70L, TRANSACTION_3.getId(), TRANSACTION_2.getId(), TRANSACTION_3.getHeight());
        REFERENCED_TRANSACTION_7 = new ReferencedTransaction(80L, TRANSACTION_4.getId(), TRANSACTION_3.getId(), TRANSACTION_4.getHeight());
        REFERENCED_TRANSACTION_8 = new ReferencedTransaction(90L, TRANSACTION_5.getId(), TRANSACTION_4.getId(), TRANSACTION_5.getHeight());
        REFERENCED_TRANSACTION_9 = new ReferencedTransaction(100L, TRANSACTION_6.getId(), TRANSACTION_5.getId(), TRANSACTION_6.getHeight());
        REFERENCED_TRANSACTION_10 = new ReferencedTransaction(110L, TRANSACTION_7.getId(), TRANSACTION_6.getId(), TRANSACTION_7.getHeight());
        REFERENCED_TRANSACTION_11 = new ReferencedTransaction(120L, TRANSACTION_8.getId(), TRANSACTION_7.getId(), TRANSACTION_8.getHeight());
        REFERENCED_TRANSACTION_12 = new ReferencedTransaction(130L, TRANSACTION_9.getId(), TRANSACTION_8.getId(), TRANSACTION_9.getHeight());
        REFERENCED_TRANSACTION_13 = new ReferencedTransaction(140L, TRANSACTION_11.getId(), TRANSACTION_8.getId(), TRANSACTION_11.getHeight());
        NOT_SAVED_REFERENCED_SHARD_TRANSACTION = new ReferencedTransaction(141L, TRANSACTION_10.getId(), TRANSACTION_11.getId(), TRANSACTION_10.getHeight());
        REFERENCED_TRANSACTIONS = Arrays.asList(REFERENCED_TRANSACTION_0, REFERENCED_TRANSACTION_1, REFERENCED_TRANSACTION_2, REFERENCED_TRANSACTION_3,
            REFERENCED_TRANSACTION_4, REFERENCED_TRANSACTION_5, REFERENCED_TRANSACTION_6, REFERENCED_TRANSACTION_7, REFERENCED_TRANSACTION_8, REFERENCED_TRANSACTION_9, REFERENCED_TRANSACTION_10, REFERENCED_TRANSACTION_11, REFERENCED_TRANSACTION_12, REFERENCED_TRANSACTION_13);
    }

    public TransactionTestData(@NonNull TransactionTypeFactory transactionTypeFactory) {
        this.transactionTypeFactory = transactionTypeFactory;
        initTransactions();
    }

    public  Transaction buildTransaction(long id, int height, long blockId, int blockTimestamp, short deadline, Long recipientId, short index, long amount, long fee, String fullHash, String signature, int timestamp, byte type, byte subtype, long senderId, String publicKey, String referencedTransactionFullhash, boolean phased, byte version, boolean hasMessage, boolean hasEncryptedMessage, boolean hasAnnouncedPublicKey, int ecBlockHeight, long ecBlockId, boolean hasEncrypttoselfMessage, boolean hasPrunableMessage, boolean hasPrunableEncryptedMessage, boolean hasPrunableAttachment, String attachment) {
        ByteBuffer buffer = null;
        if (attachment != null) {
            buffer = ByteBuffer.wrap(Convert.parseHexString(attachment));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        TransactionType transactionType = transactionTypeFactory.findTransactionType(type, subtype);
        byte[] pk = StringUtils.isBlank(publicKey) ? null : Convert.parseHexString(publicKey);
        try {
            AbstractAttachment attach = transactionType.parseAttachment(buffer);
            attach.bindTransactionType(transactionType);
            Transaction.Builder builder = new TransactionImpl.BuilderImpl(version, pk,
                amount, fee, deadline, attach, timestamp, transactionType)
                .referencedTransactionFullHash(referencedTransactionFullhash)
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
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            if (hasPrunableMessage) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            if (hasPrunableEncryptedMessage) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }

            builder.signature(SignatureToolFactory.createSignature(Convert.parseHexString(signature)));
            return builder.build();
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e);
        }

    }
}
