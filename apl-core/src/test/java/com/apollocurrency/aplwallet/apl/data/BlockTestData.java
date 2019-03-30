/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BlockTestData {
    public static final int GENESIS_BLOCK_HEIGHT = 0   ;
    public static final int BLOCK_0_HEIGHT    = 1000   ;
    public static final int BLOCK_1_HEIGHT    = 1500   ;
    public static final int BLOCK_2_HEIGHT    = 2000   ;
    public static final int BLOCK_3_HEIGHT    = 2499   ;
    public static final int BLOCK_4_HEIGHT    = 2998   ;
    public static final int BLOCK_5_HEIGHT    = 3500   ;
    public static final int BLOCK_6_HEIGHT    = 5000   ;
    public static final int BLOCK_7_HEIGHT    = 8000   ;
    public static final int BLOCK_8_HEIGHT    = 10000  ;
    public static final int BLOCK_9_HEIGHT    = 15000  ;
    public static final int BLOCK_10_HEIGHT   = 15456  ;
    public static final int BLOCK_11_HEIGHT   = 104595 ;

    public static final int GENESIS_BLOCK_TIMESTAMP = 0     ;
    public static final int BLOCK_0_TIMESTAMP       = 9200  ;
    public static final int BLOCK_1_TIMESTAMP       = 13800 ;
    public static final int BLOCK_2_TIMESTAMP       = 18400 ;
    public static final int BLOCK_3_TIMESTAMP       = 22998 ;
    public static final int BLOCK_4_TIMESTAMP       = 28098 ;
    public static final int BLOCK_5_TIMESTAMP       = 32200 ;
    public static final int BLOCK_6_TIMESTAMP       = 46000 ;
    public static final int BLOCK_7_TIMESTAMP       = 73600 ;
    public static final int BLOCK_8_TIMESTAMP       = 92000 ;
    public static final int BLOCK_9_TIMESTAMP       = 138000;
    public static final int BLOCK_10_TIMESTAMP      = 142195;
    public static final int BLOCK_11_TIMESTAMP      = 962274;

    public static final long GENESIS_BLOCK_ID = -107868771406622438L ;
    public static final long BLOCK_0_ID  = -468651855371775066L ;
    public static final long BLOCK_1_ID  = -7242168411665692630L;
    public static final long BLOCK_2_ID  = -6746699668324916965L;
    public static final long BLOCK_3_ID  = -3540343645446911906L;
    public static final long BLOCK_4_ID  =  2729391131122928659L;
    public static final long BLOCK_5_ID  =  1842732555539684628L;
    public static final long BLOCK_6_ID  = -5580266015477525080L;
    public static final long BLOCK_7_ID  =  6438949995368593549L;
    public static final long BLOCK_8_ID  =  7551185434952726924L;
    public static final long BLOCK_9_ID =  8306616486060836520L;
    public static final long BLOCK_10_ID = -6206981717632723220L;
    public static final long BLOCK_11_ID = -4166853316012435358L;

    private final TransactionTestData td = new TransactionTestData();
    public final Block GENESIS_BLOCK;
    public final Block BLOCK_0;
    public final Block BLOCK_1;
    public final Block BLOCK_2;
    public final Block BLOCK_3;
    public final Block BLOCK_4;
    public final Block BLOCK_5;
    public final Block BLOCK_6;
    public final Block BLOCK_7;
    public final Block BLOCK_8;
    public final Block BLOCK_9;
    public final Block BLOCK_10;
    public final Block BLOCK_11;

    public final List<Block> BLOCKS;

    public BlockTestData() {
        GENESIS_BLOCK = new BlockImpl(-1, GENESIS_BLOCK_TIMESTAMP, 0,                0, 0,         0,    new byte[32],                                                                               1739068987193023818L, Convert.parseHexString("bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5"), new byte[64],                                                                                                                                               new byte[32],                                                                              BigInteger.ZERO,                                             5124095, 8235640967557025109L, GENESIS_BLOCK_HEIGHT, GENESIS_BLOCK_ID, 0, Collections.emptyList());
        BLOCK_0 = new  BlockImpl(3, BLOCK_0_TIMESTAMP , 9108206803338182346L , 0, 100000000, 1255, Convert.parseHexString("37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0"), 9211698109297098287L, Convert.parseHexString("002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf"), Convert.parseHexString("e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31"), Convert.parseHexString("cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282"), new BigInteger(Convert.parseHexString("026543d9a8161629")), 9331842     ,-1868632362992335764L  ,BLOCK_0_HEIGHT   ,BLOCK_0_ID ,0, Arrays.asList(td.TRANSACTION_0, td.TRANSACTION_1));
        BLOCK_1 = new  BlockImpl(3, BLOCK_1_TIMESTAMP , -3475222224033883190L, 0, 100000000, 1257, Convert.parseHexString("2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6"), 9211698109297098287L, Convert.parseHexString("fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef"), Convert.parseHexString("978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e"), Convert.parseHexString("cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0"), new BigInteger(Convert.parseHexString("026601a7a1c313ca")), 7069966     , 5841487969085496907L  ,BLOCK_1_HEIGHT   ,BLOCK_1_ID ,0, Arrays.asList(td.TRANSACTION_2));
        BLOCK_2 = new  BlockImpl(5, BLOCK_2_TIMESTAMP , 2069655134915376442L , 0, 200000000, 207 , Convert.parseHexString("18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99"), 5564664969772495473L, Convert.parseHexString("dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b"), Convert.parseHexString("4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4"), Convert.parseHexString("3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e"), new BigInteger(Convert.parseHexString("02dfb5187e88edab")), 23058430050L,-3540343645446911906L  ,BLOCK_2_HEIGHT   ,BLOCK_2_ID ,1, Arrays.asList(td.TRANSACTION_3));
        BLOCK_3 = new  BlockImpl(6, BLOCK_3_TIMESTAMP , -6746699668324916965L, 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),-902424482979450876L , Convert.parseHexString("facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3"), Convert.parseHexString("f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e"), Convert.parseHexString("1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e"), new BigInteger(Convert.parseHexString("02dfb518ae37f5ac")), 23058430050L, 2729391131122928659L  ,BLOCK_3_HEIGHT   ,BLOCK_3_ID ,4, Collections.emptyList());
        BLOCK_4 = new  BlockImpl(6, BLOCK_4_TIMESTAMP , -3540343645446911906L, 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), 4363726829568989435L, Convert.parseHexString("1fc63f083c3a49042c43c22a7e4d92aadac95c78d06ed21b8f9f0efd7c23b2a1"), Convert.parseHexString("8cb8795d7a320e693e64ea67b47348f2f1099c3e7163311b59135aaff1a78a00542b9d4713928c265666997a4ce63b0c07585ce04464f8dfb2253f21f91bf22e"), Convert.parseHexString("5e485346362cdece52dada076459abf88a0ae128cac6870e108257a88543f09f"), new BigInteger(Convert.parseHexString("02dfb518dde6fdad")), 23058430050L, 1842732555539684628L  ,BLOCK_4_HEIGHT   ,BLOCK_4_ID ,3, Collections.emptyList());
        BLOCK_5 = new  BlockImpl(4, BLOCK_5_TIMESTAMP , 2729391131122928659L , 0, 200000000, 207 , Convert.parseHexString("9a8d7e4f2e83dc49351f9c3d72fabc5ecdc75f6eccc2b90f147ff5ec7d5068b2"), 4363726829568989435L, Convert.parseHexString("a042a2accbb2600530a4df46db4eba105ac73f4491923fb1c34a6b9dd2619634"), Convert.parseHexString("ee4e2ccd12b36ade6318b47246ddcad237a153da36ab9ea2498373a4687c35072f2a9d49925520b588cb16d0e5663f3d10e3adeee97dcbbb4137470e521b347c"), Convert.parseHexString("130cafd7c5bee025885d0c6b58b2ddaaed71d2fa48423f552eb5828a423cc94b"), new BigInteger(Convert.parseHexString("02dfb5190d9605ae")), 23058430050L,-5580266015477525080L  ,BLOCK_5_HEIGHT   ,BLOCK_5_ID ,0, Arrays.asList(td.TRANSACTION_4, td.TRANSACTION_5, td.TRANSACTION_6));
        BLOCK_6 = new  BlockImpl(6, BLOCK_6_TIMESTAMP , 1842732555539684628L , 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),-6535098620285495989L, Convert.parseHexString("20feb26c8c34c22d55de747e3964acb3bc864326736949876d2b0594d15e87dd"), Convert.parseHexString("a22f758567f0bd559ce2d821399da4f9ffdc4a694057d8b37045d2a9222be405f4311938e88a0b56418cbadcbea47dadabfc16e58f74e5dcd7a975d95dc17766"), Convert.parseHexString("149dfdfc7eb39219330d620a14fb0c2f02369abbda562bc4ab068e90c3cf11a4"), new BigInteger(Convert.parseHexString("02dfb5193d450daf")), 23058430050L, 6438949995368593549L  ,BLOCK_6_HEIGHT   ,BLOCK_6_ID ,7, Collections.emptyList());
        BLOCK_7 = new  BlockImpl(4, BLOCK_7_TIMESTAMP , -5580266015477525080L, 0, 200000000, 207 , Convert.parseHexString("8bdf98fbc4cfcf0b66dfaa688ce7ef9063f8b1748ee238c23e8209f071cfcee7"), 6415509874415488619L, Convert.parseHexString("5b1bf463f202ec0d4ab42a9634976ed47b77c462d1de25e3fea3e8eaa8add8f6"), Convert.parseHexString("992eacb8ac3bcbb7dbdbfcb637318adab190d4843b00da8961fd36ef60718f0f5acca4662cfdcf8447cc511d5e36ab4c321c185382f3577f0106c2bfb9f80ee6"), Convert.parseHexString("a81547db9fe98eb224d3cdc120f7305d3b829f162beb3bf719750e0cf48dbe9d"), new BigInteger(Convert.parseHexString("02dfb5196cf415b0")), 23058430050L, 7551185434952726924L  ,BLOCK_7_HEIGHT   ,BLOCK_7_ID ,0, Arrays.asList(td.TRANSACTION_7, td.TRANSACTION_8));
        BLOCK_8 = new  BlockImpl(6, BLOCK_8_TIMESTAMP , 6438949995368593549L , 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), 7160808267188566436L, Convert.parseHexString("1435d4b603b52d04dd0f8228f36dbd6f01e627a59370fa3e6a0f58a75b372621"), Convert.parseHexString("5a8acc3cc947b76d42fa78938ed9ece33b91c5ca0bb7a1af6c92ec525e8bb6092babf03aee10bd965123fceb5afad63969e78991d8c6b2a6b4fc79cff8fe150d"), Convert.parseHexString("8db872e0e7be5b59fb68ef26d84bfeb9df04f6a5b6f701fd1c88578bfcf48a84"), new BigInteger(Convert.parseHexString("02dfb5199ca31db1")), 23058430050L, 8306616486060836520L  ,BLOCK_8_HEIGHT   ,BLOCK_8_ID ,6, Collections.emptyList());
        BLOCK_9  = new BlockImpl(6, BLOCK_9_TIMESTAMP, 7551185434952726924L , 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),-3985647971895643754L, Convert.parseHexString("5f1382ab768b8b000637d8a59d7415fd8d4b6d4edc00ca0a08aacf9caf8c9a06"), Convert.parseHexString("6832f74bd4abe2a4b95755ff9e989133079e5215ae2111e590ea489353ce28078d094db3db077124ac541be9f4f7f09f5a36aac83c8c151dae0f09eb378033e1"), Convert.parseHexString("8cf9752b2533cb6849ad83b275c40f7e61b204ac023f775847a60c2f1a9d3d79"), new BigInteger(Convert.parseHexString("02dfb519cc5225b2")), 23058430050L,-6206981717632723220L   ,BLOCK_9_HEIGHT   ,BLOCK_9_ID,9, Collections.emptyList());
        BLOCK_10 = new BlockImpl(4, BLOCK_10_TIMESTAMP, 8306616486060836520L , 0, 200000000, 207 , Convert.parseHexString("550dfe6da8732c1977c7545675f8dc163995aaba5533306b7a1f1b9364190dd3"), 4749500066832760520L, Convert.parseHexString("df545469ed5a9405e0ff6efcdf468e61564776568c8b227f776f24c47206af46"), Convert.parseHexString("3d1c22000eb41599cb12dfbfaa3980353fa84cdf99145d1fcc92886551044a0c0b388c539efa48414c21251e493e468d97a2df12be24e9a33dec4521fdb6c2eb"), Convert.parseHexString("a8460f09af074773186c58688eb29215a81d5b0b10fc9e5fc5275b2f39fd93bb"), new BigInteger(Convert.parseHexString("02dfb519fc012db3")), 23058430050L,-4166853316012435358L  ,BLOCK_10_HEIGHT  ,BLOCK_10_ID,0,Arrays.asList(td.TRANSACTION_9, td.TRANSACTION_10, td.TRANSACTION_11, td.TRANSACTION_12));
        BLOCK_11 = new BlockImpl(6, BLOCK_11_TIMESTAMP, -6206981717632723220L, 0, 0,         0   , Convert.parseHexString("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),3883484057046974168L, Convert.parseHexString("82e59d851fdf0d01ca1ee20df906009cd66885cc63e8314ebde80dc5e38987fa"), Convert.parseHexString("202acda4d57f2a24212d265053241a07608de29a6dd8252994cf8be197765d02a585c676aca15e7f43a57d7747173d51435d9f2820da637ca8bc9cd1e536d761"), Convert.parseHexString("ec562889035fdca9d59d9bdca460992c01c5286278104287a989834eeffcb83e"), new BigInteger(Convert.parseHexString("02dfb51a2bb035b4")), 23058430050L, 433871417191886464L    ,BLOCK_11_HEIGHT  ,BLOCK_11_ID,9, Collections.emptyList());

        BLOCKS = Arrays.asList(GENESIS_BLOCK, BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3, BLOCK_4, BLOCK_5, BLOCK_6, BLOCK_7, BLOCK_8, BLOCK_9, BLOCK_10, BLOCK_11);
    }
}
