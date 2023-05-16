/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.List;

public class PrunableMessageTestData {
    public final byte[] MESSAGE_1_SHARED_KEY = Convert.parseHexString("668afea67b335ac07360ce6219dea989654d18ff595de7f769b76ce886d0b227");
    public final byte[] MESSAGE_4_SHARED_KEY = Convert.parseHexString("4db705d998f8def1a4e507ea6ff5d6e59a2f4e47f3bd281e8d4ff54ed84b7d03");
    // DATA_1_ABTC
    //    DATA_1 - name of data
    //    A - first letter of sender name (ALICE)
    //    B - first letter of recipient name (BOB)
    //    T - means, that message is text, B - message is binary
    //    C - means, that message was compressed
    //
    public final EncryptedData DATA_1_ABTC = new EncryptedData(Convert.parseHexString("b63f809f0df0b52f46225b702855c2704653e88ae96cd3dfe2c50cf2e30f747907cbb06267616241aa3aa55c2bf90457b1f275e9c96d42c7cc73cdb229a4fed055ad55245c89348c0d05c757e771996d"), Convert.parseHexString("8d597125aabb471bb25cd2d6e524f1bb9811c40f8259eff2cadf1f48df5c06f3"));
    public final EncryptedData DATA_2_BATC = new EncryptedData(Convert.parseHexString("7ac5c6571b768208df62ee48d3e9c8151abeb0f11858c66d01bdf0b8170b0741b596da28500094b25ed0bb981a41f4dfe489128c4013638d5c8eb143946b6af77b64da893560374409866b0db539ff45"), Convert.parseHexString("6bbe56de583181db3ac90d67ee6f16bc0be3faa400e03ef25616b45789fde2ab"));
    public final EncryptedData DATA_3_ABT_ = new EncryptedData(Convert.parseHexString("dd39282b7262b9369773b68a851491bbecac1f9b7a6ec078381962b129872c2df5ee11b489ef1733e78c6c54fb6fbcf992071fdb83c4e40f501b8101af76dae9"), Convert.parseHexString("c61c3726d86490c43955644a64aa004d4fa45184e37060247a9a535acdc638ac"));
    public final EncryptedData DATA_4_BAT_ = new EncryptedData(Convert.parseHexString("dae6440045c8d5bf5c561d5ed9209898654038bb375875e5a50bf0d7bb44bdcaf4c074354638aa0fe97d70d4cb00a6d62c119703b75f63d40a29190feb85ba54"), Convert.parseHexString("d9e8e433e07bfcea5923e0ff59a0e8fd3c9bdd7bcd76a08eb5bcec871c65d06f"));
    public final EncryptedData DATA_5_AATC = new EncryptedData(Convert.parseHexString("7f39dde4494bdd8036799dc04d2e7967c3cc40af2fd3a0bd40e5113076713b9f2aa6895b6f848bfafce0fc085c991d0f0883ef75fe8b75e3bcf9308d4de27837958436fb572cf954a3dff1523908d4d0"), Convert.parseHexString("9ff85558cb2bcd6ac2bba4967c4ae9c6fca25f4f8313b53b3aec308e02e3f758"));
    public final EncryptedData DATA_6_BBTC = new EncryptedData(Convert.parseHexString("a8d8a1784144872e909f5c5c0eb75ea01fc45a3a0aba2f04bbe8bc29414ab1d82c617d184baf26d4dab5f6e584326eb7a69649d70906cbae8a8633c59b5357b5f19ab6a1bcc94939b33723c192c734f6"), Convert.parseHexString("2886a8ad860d8bcd23398545d04776d33401adbdf1f4b72d669388ade4cc759c"));
    public final EncryptedData DATA_7_CAT_ = new EncryptedData(Convert.parseHexString("11c48e4daac8e8582c9b83715366a0a0b4a7b7ae048d0ad115d22ae973c9c9e255fbb70f1b17168f6d15d877fa4dfd9017c8aedc9211e4576e434fb4b7102776"), Convert.parseHexString("777164f79368343936dd87f65dd58b24f61b075973c7b7c5947e5020bc835baf"));
    public final EncryptedData DATA_8_CBB = new EncryptedData(Convert.parseHexString("8de2b1bb43fc8f8ed866f551edae2f688494da7601b914fbc69f2c9c406f537845eab9a324a151d432d82a0e9d989467b1ff559a947fe8a5d0c9fe7bf0e6d0a4"), Convert.parseHexString("4504273ff6b92b419abf752401b785157eb320f78e6ac13f75036a799ea47a4c"));
    public final EncryptedData DATA_9_BCBC = new EncryptedData(Convert.parseHexString("a1e59a83f92fe32e2e8bd4d840adca3af792e65499ae3d87068c793daf7f7d238c9c0820c951a9280d78e492eb27fb5961a974d98f63756728cb7a22d658dabbc0c6bf192eea4f41d950cff9f51c12f0"), Convert.parseHexString("3f2f853cd9ead88f3c88ebbdb1ae0423dad64b3d2c0801fc1780b41c84fc330e"));
    public final String PUBLIC_MESSAGE_1_BAT = "Hi, Alice!";
    public final String PUBLIC_MESSAGE_2_ABT = "Goodbye, Bob!";
    public final String PUBLIC_MESSAGE_3_BAB = "ff3456236527fdab";
    public final String PUBLIC_MESSAGE_4_CAT = "Test message to Alice";
    public final String PUBLIC_MESSAGE_5_CBB = "f3ab4384a18c2911";
    public final String PUBLIC_MESSAGE_6_ACT = "Hello Chuck";
    public final String PUBLIC_MESSAGE_BYTES_1 = "48692c20416c69636521";
    public final String PUBLIC_MESSAGE_BYTES_2 = "476f6f646279652c20426f6221";
    public final String PUBLIC_MESSAGE_BYTES_4 = "54657374206d65737361676520746f20416c696365";
    public final String PUBLIC_MESSAGE_BYTES_6 = "48656c6c6f20436875636b";
    public final String DECRYPTED_MESSAGE_1 = "alice_to_bob encrypted compressed text message";
    public final String DECRYPTED_MESSAGE_2 = "bob_to_alice encrypted compressed text message";
    public final String DECRYPTED_MESSAGE_3 = "alice_to_bob encrypted text message";
    public final String DECRYPTED_MESSAGE_4 = "bob_to_alice encrypted text message";
    public final String DECRYPTED_MESSAGE_5 = "alice_to_alice compressed encrypted text message";
    public final String DECRYPTED_MESSAGE_6 = "bob_to_bob compressed encrypted text message";
    public final String DECRYPTED_MESSAGE_7 = "chuck_to_alice encrypted text message";
    public final String DECRYPTED_MESSAGE_9 = "chuck_to_bob encrypted binary message";
    public final String DECRYPTED_MESSAGE_10 = "bob_to_chuck encrypted binary message";
    public String ALICE_PASSPHRASE = "alice";
    public String BOB_PASSPHRASE = "bob";
    public String CHUCK_PASSPHRASE = "chuck";
    public String ALICE_RS = "APL-ZW95-E7B5-MVVP-CCBDT";
    public String BOB_RS = "APL-CJHA-9RWR-MJ42-6DCJ9";
    public String CHUCK_RS = "APL-Y6S2-BU3L-SZXP-CV82X";
    public String ALICE_PUBLIC_KEY = "2e8b2883c27b391359d7d0c15d00815a6693290a38dd8eba341cc07ddaa8ed4d";
    public String BOB_PUBLIC_KEY = "c0b126b5aa134b84e64625256b8de8973787612b1f3839d2c60526d2114e886d";
    public String CHUCK_PUBLIC_KEY = "a574702cf92b7e0a7847f921ca8a23f538d62a582cc6a2f4861b160077c5f245";
    public long ALICE_ID = -6004096130734886685L;
    public final PrunableMessage MESSAGE_5 = new PrunableMessage(1040L, 50, ALICE_ID, ALICE_ID, null, DATA_5_AATC, false, true, true, 180, 168, 18);
    public long BOB_ID = 4882266200596627944L;
    public final PrunableMessage MESSAGE_1 = new PrunableMessage(1000L, 10, ALICE_ID, BOB_ID, null, DATA_1_ABTC, false, true, true, 128, 120, 10);
    public final PrunableMessage MESSAGE_2 = new PrunableMessage(1010L, 20, BOB_ID, ALICE_ID, Convert.parseHexString(PUBLIC_MESSAGE_BYTES_1), DATA_2_BATC, true, true, true, 140, 130, 12);
    public final PrunableMessage MESSAGE_3 = new PrunableMessage(1020L, 30, ALICE_ID, BOB_ID, Convert.parseHexString(PUBLIC_MESSAGE_BYTES_2), DATA_3_ABT_, true, true, false, 158, 155, 14);
    public final PrunableMessage MESSAGE_4 = new PrunableMessage(1030L, 40, BOB_ID, ALICE_ID, Convert.parseHexString(PUBLIC_MESSAGE_3_BAB), DATA_4_BAT_, false, true, false, 160, 157, 15);
    public final PrunableMessage MESSAGE_7 = new PrunableMessage(1060L, 70, BOB_ID, BOB_ID, null, DATA_6_BBTC, false, true, true, 211, 214, 22);
    public long CHUCK_ID = -5872452783836294400L;
    public final PrunableMessage MESSAGE_6 = new PrunableMessage(1050L, 60, CHUCK_ID, ALICE_ID, Convert.parseHexString(PUBLIC_MESSAGE_BYTES_4), null, true, false, false, 185, 178, 19);
    public final PrunableMessage MESSAGE_8 = new PrunableMessage(1070L, 80, CHUCK_ID, ALICE_ID, null, DATA_7_CAT_, false, true, false, 212, 225, 23);
    public final PrunableMessage MESSAGE_9 = new PrunableMessage(1080L, 90, CHUCK_ID, BOB_ID, Convert.parseHexString(PUBLIC_MESSAGE_5_CBB), DATA_8_CBB, false, false, false, 232, 230, 25);
    public final PrunableMessage MESSAGE_10 = new PrunableMessage(1090L, 100, BOB_ID, CHUCK_ID, null, DATA_9_BCBC, false, false, true, 247, 242, 28);
    public final PrunableMessage MESSAGE_11 = new PrunableMessage(1100L, 110, ALICE_ID, CHUCK_ID, Convert.parseHexString(PUBLIC_MESSAGE_BYTES_6), null, true, false, false, 259, 254, 30);
    public final List<PrunableMessage> ALL = List.of(
        MESSAGE_1,
        MESSAGE_2,
        MESSAGE_3,
        MESSAGE_4,
        MESSAGE_5,
        MESSAGE_6,
        MESSAGE_7,
        MESSAGE_8,
        MESSAGE_9,
        MESSAGE_10,
        MESSAGE_11
    );
    public final PrunableMessage NEW_MESSAGE = new PrunableMessage(1101L, 120, CHUCK_ID, ALICE_ID, Convert.parseHexString("f348294357"), null, false, false, false, 269, 258, 31);
}
