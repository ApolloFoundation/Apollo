/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderType;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;

public class DexTestData {
    public static final long EXCHANGE_CONTRACT_ID_1 = -3625894990594689368L;
    public static final long EXCHANGE_CONTRACT_ID_2 = -7277152511870517934L;
    public static final long EXCHANGE_CONTRACT_ID_3 = 8455581613897449491L;
    public static final long EXCHANGE_CONTRACT_ID_4_5 = 7952648026362992483L;
    public static final long EXCHANGE_CONTRACT_ID_6 = 8455581613897449491L;
    public static final long EXCHANGE_CONTRACT_ID_7 = -6988530272040477515L;
    public static final long EXCHANGE_CONTRACT_ID_8 = 4590047955464765433L;
    public static final long EXCHANGE_CONTRACT_ID_9 = -1620743079267768652L;
    public static final long EXCHANGE_CONTRACT_ID_10 = 664254800608944568L;
    public static final long EXCHANGE_CONTRACT_ID_11 = -139104235169499924L;
    public static final long EXCHANGE_CONTRACT_ID_12 = -4808040955344135102L;
    public static final long EXCHANGE_CONTRACT_ID_13 = -4084842872841996828L;
    public static final long EXCHANGE_CONTRACT_ID_14 = 1035081890238412012L;
    public static final long EXCHANGE_CONTRACT_ID_15 = -2471101731518812718L;
    public static final long DEX_TRADE_ID_1 = -3625894990594689368L;
    private static final int DEFAULT_TIME = 7200;
    public final long ALICE = 100;
    public final long BOB = 200;
    // type(Buy/Sell currency (ETH/PAX) account (Alice/BOB)
    public final DexOrder ORDER_BEA_1 = new DexOrder(1000L, 1L, OrderType.BUY, 100L, DexCurrency.APL, 500000L, DexCurrency.ETH, BigDecimal.valueOf(0.001), 6000, OrderStatus.CLOSED, 100, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final DexOrder ORDER_SPA_2 = new DexOrder(1010L, 2L, OrderType.SELL, 100L, DexCurrency.APL, 200000L, DexCurrency.PAX, BigDecimal.valueOf(0.16), 6500, OrderStatus.CANCEL, 110, "APL-K78W-Z7LR-TPJY-73HZK", "0x602242c68640e754677b683e20a2740f8f95f7d3");
    public final DexOrder ORDER_BPB_1 = new DexOrder(1020L, 3L, OrderType.BUY, 200L, DexCurrency.APL, 100000L, DexCurrency.PAX, BigDecimal.valueOf(0.15), 7000, OrderStatus.OPEN, 121, "0x777BE94ea170AfD894Dd58e9634E442F6C5602EF", "APL-T69E-CTDG-8TYM-DKB5H");
    public final DexOrder ORDER_SEA_3 = new DexOrder(1030L, 4L, OrderType.SELL, 100L, DexCurrency.APL, 400000L, DexCurrency.ETH, BigDecimal.valueOf(0.001), 8000, OrderStatus.WAITING_APPROVAL, 121, "APL-K78W-Z7LR-TPJY-73HZK", "0x602242c68640e754677b683e20a2740f8f95f7d3");
    public final DexOrder ORDER_BEA_4 = new DexOrder(1040L, 5L, OrderType.BUY, 100L, DexCurrency.APL, 600000L, DexCurrency.ETH, BigDecimal.valueOf(0.001), 11000, OrderStatus.OPEN, 122, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final DexOrder ORDER_BPA_5 = new DexOrder(1050L, 6L, OrderType.BUY, 100L, DexCurrency.APL, 400000L, DexCurrency.PAX, new BigDecimal("0.00001"), 13001, OrderStatus.CLOSED, 123, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final DexOrder ORDER_BEA_6 = new DexOrder(1060L, 7L, OrderType.BUY, 100L, DexCurrency.APL, 500000L, DexCurrency.ETH, BigDecimal.valueOf(0.054), 15001, OrderStatus.WAITING_APPROVAL, 123, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final DexOrder ORDER_BPB_2 = new DexOrder(1070L, 8L, OrderType.BUY, 200L, DexCurrency.APL, 44000L, DexCurrency.PAX, BigDecimal.valueOf(0.00043), 16001, OrderStatus.PENDING, 123, "0x777BE94ea170AfD894Dd58e9634E442F6C5602EF", "APL-T69E-CTDG-8TYM-DKB5H");
    public final DexOrder ORDER_SEA_7 = new DexOrder(1080L, 9L, OrderType.SELL, 100L, DexCurrency.APL, 43000L, DexCurrency.ETH, BigDecimal.valueOf(0.0076), 17001, OrderStatus.CLOSED, 124, "APL-K78W-Z7LR-TPJY-73HZK", "0x602242c68640e754677b683e20a2740f8f95f7d3");
    public final DexOrder ORDER_BEA_8 = new DexOrder(1090L, 10L, OrderType.BUY, 100L, DexCurrency.APL, 6550000L, DexCurrency.ETH, BigDecimal.valueOf(0.0074), 19001, OrderStatus.CLOSED, 124, "0x602242c68640e754677b683e20a2740f8f95f7d3", "APL-K78W-Z7LR-TPJY-73HZK");
    public final ExchangeContract EXCHANGE_CONTRACT_1;
    public final ExchangeContract EXCHANGE_CONTRACT_2;
    public final ExchangeContract EXCHANGE_CONTRACT_3;
    public final ExchangeContract EXCHANGE_CONTRACT_4;
    public final ExchangeContract EXCHANGE_CONTRACT_5;
    public final ExchangeContract EXCHANGE_CONTRACT_6;
    public final ExchangeContract EXCHANGE_CONTRACT_7;
    public final ExchangeContract EXCHANGE_CONTRACT_8;
    public final ExchangeContract EXCHANGE_CONTRACT_9;
    public final ExchangeContract EXCHANGE_CONTRACT_10;
    public final ExchangeContract EXCHANGE_CONTRACT_11;
    public final ExchangeContract EXCHANGE_CONTRACT_12;
    public final ExchangeContract EXCHANGE_CONTRACT_13;
    public final ExchangeContract EXCHANGE_CONTRACT_14;
    public final ExchangeContract EXCHANGE_CONTRACT_15;
    public final ExchangeContract NEW_EXCHANGE_CONTRACT_16;
    public final long NEW_EXCHANGE_CONTRACT_ID = -762612439991997299L;
    public final long NEW_EXCHANGE_CONTRACT_SENDER_ID = -382612439991997299L;
    public final long NEW_EXCHANGE_CONTRACT_RECIPIENT_ID = 4582612439991997299L;
    // contract attachment
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_1;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_2;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_3;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_4;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_5;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_6;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_7;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_8;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_9;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_10;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_11;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_12;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_13;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_14;
    public final DexContractAttachment DEX_CONTRACT_ATTACHMENT_15;
    public final DexContractAttachment NEW_DEX_CONTRACT_ATTACHMENT_4;
    public final DexTransaction TX_1 = new DexTransaction(100L, Numeric.hexStringToByteArray("0xa69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26"), Numeric.hexStringToByteArray("0xff"), DexTransaction.Op.DEPOSIT, "100", "0x0398E119419E0D7792c53913d3f370f9202Ae137", 250);
    public final DexTransaction TX_2 = new DexTransaction(200L, Numeric.hexStringToByteArray("0x203b36aac62037ac7c4502aa023887f7fcae843c456fde083e6a1dc70a29f3d61a73f57d79481f06e27ea279c74528e1ba6b1854d219b1e3b255729889ca5926"), Numeric.hexStringToByteArray("0xff"), DexTransaction.Op.INITIATE, "100", "0x0398E119419E0D7792c53913d3f370f9202Ae137", 300);
    public final DexTransaction TX_3 = new DexTransaction(300L, Numeric.hexStringToByteArray("0x05ae03fd135de159cc512d0a34317d0c5270fc9d0c02ebc648828dec221272d8f20f83485bb16d0dc58acbc4a84ccc8363ef7413885936c8ee7cc943ef65cbd1"), Numeric.hexStringToByteArray("0xff"), DexTransaction.Op.DEPOSIT, "102", "0x0398E119419E0D7792c53913d3f370f9202Ae137", 400);

    public DexTestData() {

        DEX_CONTRACT_ATTACHMENT_1 = new DexContractAttachment(-5227805726286506078L, -7138882269097972721L,
            Convert.parseHexString("f41a9d03745d78c8efd682b4f6030fd70623e5c38ae2115d53f2c94f483aa121"), null, "0x73949de85a63ed24457fc4188c8876726024a3f67fa673389a7aae47698e61bd",
            Convert.parseHexString("b4f38c90ab6f36fc76013a7a69152186e2c44ef73d188a041770c253d6ccd1b88e24f37ab3c0bfd77fc74a4600c4090aea1dc1a297a2aa3400a330cb6f670fec"),
            ExchangeContractStatus.STEP_1, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_2 = new DexContractAttachment(4066034979755747272L, 6794334481055229134L,
            Convert.parseHexString("8e0f875179dd784241babdc56e1380370620db1c8aa1b7f765e2b98cd3fc2840"), "12380311258696115355", "0xe50bd6b4c62d8fb167de66c11a7a57cbcc97a2e945ddd3829d7cf0f09fda7b14",
            Convert.parseHexString("e670c46452e18fe2224edf5fba888affef6060e0efeeb10862bcfdebfcfcf997dc37443b1ff44c79977f484e4b4e2e94404620145ebeee5bce7a2f609b453e13"),
            ExchangeContractStatus.STEP_3, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_3 = new DexContractAttachment(5339180579805180746L, -5842203753269117069L,
            Convert.parseHexString("509520c8d27b08b9208b38f6ab1735c043263c18d2579a44f2210135ca92b480"), "0x8540339763b19265f394140544fe060711b1e0623860d8b99e21ffc769574f50", "4340657620930323843",
            Convert.parseHexString("d6e6c72256548595c331c66d0d3fb5b1141b26e2d15946092acb3e3e46b781f7f52148408a9f0d845333cccab9c822f13149eae2ab5b963c921e4a7e97dabd7f"),
            ExchangeContractStatus.STEP_2, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_4 = new DexContractAttachment(6735355323156389437L, 3332621836748752862L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_1, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_5 = new DexContractAttachment(6735355323156389437L, 3332621836748752862L,
            Convert.parseHexString("509520c8d27b08b9208b38f6ab1735c043263c18d2579a44f2210135ca92b480"), null, "100",
            Convert.parseHexString("d6e6c72256548595c331c66d0d3fb5b1141b26e2d15946092acb3e3e46b781f7f52148408a9f0d845333cccab9c822f13149eae2ab5b963c921e4a7e97dabd7f"),
            ExchangeContractStatus.STEP_2, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_6 = new DexContractAttachment(-2195048504635381606L, 6188327480147022018L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_1, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_7 = new DexContractAttachment(-5716376597917953548L, 3332621836748752862L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_1, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_8 = new DexContractAttachment(6876238954523300917L, -4688237877525140429L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_1, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_9 = new DexContractAttachment(-5147790389386504951L, -5517784857671387693L,
            Convert.parseHexString("2c92403a45334780593a5d0f9e443273cd026abe43cfae47fa5d8e69c278c064"), "0xa5c635cb164272ceb29ff055e8a6d0b2061dd886304d8b3f08800e3f4e76d3fa", "15853180921951477110",
            Convert.parseHexString("22cd2c8d73ab0544a872963f013089017818d158fcb036040d2e411b6c80425a0eb0531252bd686892ea1ad84a54fdd7d5ce97122a903b3f3e536feb98d01a3a"),
            ExchangeContractStatus.STEP_3, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_10 = new DexContractAttachment(-8286957857729261741L, 1767521844898370512L,
            Convert.parseHexString("aec6592aaa4de756e64451aba44361327fd766f403892f4c7502659a56d27bb8"), "6606192650543722486", "0xd0349ff2fb66d88d9c6f788b5b80b5c22aeafac349a627e1089c4305210b479d",
            Convert.parseHexString("d039be0cf1318f301a8b908a3b54b1572f0db3be40d7cf7d5bb263ac68eaa05ad4f8300870d7baabab86c62199e4162c6eefe1f36b6533d7b48951087015dff4"),
            ExchangeContractStatus.STEP_3, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_11 = new DexContractAttachment(-2946834708490131834L, -6968465014361285240L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_4, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_12 = new DexContractAttachment(-7670014354885567965L, -6968465014361285240L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_4, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_13 = new DexContractAttachment(-431466151140031473L, -6968465014361285240L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_4, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_14 = new DexContractAttachment(4573417476053711227L, -6968465014361285240L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_4, DEFAULT_TIME);
        DEX_CONTRACT_ATTACHMENT_15 = new DexContractAttachment(8603248567538608464L, -6968465014361285240L,
            null, null, null,
            null,
            ExchangeContractStatus.STEP_4, DEFAULT_TIME);

        EXCHANGE_CONTRACT_1 = new ExchangeContract(10L, EXCHANGE_CONTRACT_ID_1, -582612439131997299L, -582612439131997299L, 53499864, DEX_CONTRACT_ATTACHMENT_1, 100, false);
        EXCHANGE_CONTRACT_2 = new ExchangeContract(20L, EXCHANGE_CONTRACT_ID_2, 7477442401604846627L, 7477442401604846627L, 53499868, DEX_CONTRACT_ATTACHMENT_2, 200, true);
        EXCHANGE_CONTRACT_3 = new ExchangeContract(30L, EXCHANGE_CONTRACT_ID_3, 7477442401604846627L, -582612439131997299L, 53499882, DEX_CONTRACT_ATTACHMENT_3, 300, true);
        EXCHANGE_CONTRACT_4 = new ExchangeContract(40L, EXCHANGE_CONTRACT_ID_4_5, 7477442401604846627L, -582612439131997299L, 53499983, DEX_CONTRACT_ATTACHMENT_4, 400, false);
        EXCHANGE_CONTRACT_5 = new ExchangeContract(50L, EXCHANGE_CONTRACT_ID_4_5, 7477442401604846627L, -582612439131997299L, 53500038, DEX_CONTRACT_ATTACHMENT_5, 401, true);
        EXCHANGE_CONTRACT_6 = new ExchangeContract(60L, EXCHANGE_CONTRACT_ID_6, 7477442401604846627L, -582612439131997299L, 53500042, DEX_CONTRACT_ATTACHMENT_6, 500, false);
        EXCHANGE_CONTRACT_7 = new ExchangeContract(70L, EXCHANGE_CONTRACT_ID_7, 7477442401604846627L, -582612439131997299L, 53500057, DEX_CONTRACT_ATTACHMENT_7, 500, false);
        EXCHANGE_CONTRACT_8 = new ExchangeContract(80L, EXCHANGE_CONTRACT_ID_8, -582612439131997299L, 7477442401604846627L, 53497715, DEX_CONTRACT_ATTACHMENT_8, 500, false);
        EXCHANGE_CONTRACT_9 = new ExchangeContract(90L, EXCHANGE_CONTRACT_ID_9, -582612439131997299L, 7477442401604846627L, 53497244, DEX_CONTRACT_ATTACHMENT_9, 600, true);
        EXCHANGE_CONTRACT_10 = new ExchangeContract(100L, EXCHANGE_CONTRACT_ID_10, -582612439131997299L, 7477442401604846627L, 53497244, DEX_CONTRACT_ATTACHMENT_10, 700, true);
        EXCHANGE_CONTRACT_11 = new ExchangeContract(110L, EXCHANGE_CONTRACT_ID_11, -582612439131997299L, 7477442401604846627L, 53497122, DEX_CONTRACT_ATTACHMENT_11, 800, true);
        EXCHANGE_CONTRACT_12 = new ExchangeContract(120L, EXCHANGE_CONTRACT_ID_12, -582612439131997299L, 7477442401604846627L, 53497141, DEX_CONTRACT_ATTACHMENT_12, 800, true);
        EXCHANGE_CONTRACT_13 = new ExchangeContract(130L, EXCHANGE_CONTRACT_ID_13, -582612439131997299L, 7477442401604846627L, 53497194, DEX_CONTRACT_ATTACHMENT_13, 800, true);
        EXCHANGE_CONTRACT_14 = new ExchangeContract(140L, EXCHANGE_CONTRACT_ID_14, -582612439131997299L, 7477442401604846627L, 53497211, DEX_CONTRACT_ATTACHMENT_14, 800, true);
        EXCHANGE_CONTRACT_15 = new ExchangeContract(150L, EXCHANGE_CONTRACT_ID_15, -582612439131997299L, 7477442401604846627L, 53497245, DEX_CONTRACT_ATTACHMENT_15, 800, true);

        NEW_DEX_CONTRACT_ATTACHMENT_4 = new DexContractAttachment(5339180579805180746L, -5842203753269117069L,
            Convert.parseHexString("509520c8d27b08b9208b38f6ab1735c043263c18d2579a44f2210135ca92b480"), "0x8540339763b19265f394140544fe060711b1e0623860d8b99e21ffc769574f50", "4340657620930323843",
            Convert.parseHexString("d6e6c72256548595c331c66d0d3fb5b1141b26e2d15946092acb3e3e46b781f7f52148408a9f0d845333cccab9c822f13149eae2ab5b963c921e4a7e97dabd7f"),
            ExchangeContractStatus.STEP_2, DEFAULT_TIME);

        NEW_EXCHANGE_CONTRACT_16 = new ExchangeContract(NEW_EXCHANGE_CONTRACT_ID, NEW_EXCHANGE_CONTRACT_SENDER_ID, NEW_EXCHANGE_CONTRACT_RECIPIENT_ID, 53499882, NEW_DEX_CONTRACT_ATTACHMENT_4);

    }
}
