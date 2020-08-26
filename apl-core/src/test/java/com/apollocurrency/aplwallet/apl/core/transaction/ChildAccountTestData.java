/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.util.List;

public class ChildAccountTestData {
    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    public static final String SENDER_SECRET_PHRASE = "here we go again";
    public static final String SENDER_ACCOUNT_RS = "APL-XR8C-K97J-QDZC-3YXHE";
    public static final String SENDER_PUBLIC_KEY = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    public static final long SENDER_ID = Convert.parseUnsignedLong("1603209491393731786");
    private static final long LOCAL_ONE_APL = 100000000;
    public static final Account SENDER = new Account(Convert.parseAccountId(SENDER_ACCOUNT_RS), 1000 * LOCAL_ONE_APL, 100 * LOCAL_ONE_APL, 0L, 0L, 0);

    public static final String CHILD_SECRET_PHRASE_1 = "1234567890";
    public static final byte[] CHILD_PUBLIC_KEY_1 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_1);
    public static final long CHILD_ID_1 = AccountService.getId(CHILD_PUBLIC_KEY_1);

    public static final String CHILD_SECRET_PHRASE_2 = "0987654321";
    public static final byte[] CHILD_PUBLIC_KEY_2 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_2);
    public static final long CHILD_ID_2 = AccountService.getId(CHILD_PUBLIC_KEY_2);

    public static final Account CHILD_1 = new Account(CHILD_ID_1, 0L, 0L, 0L, 0L, 0);
    public static final Account CHILD_2 = new Account(CHILD_ID_2, 0L, 0L, 0L, 0L, 0);

    public static final ChildAccountAttachment CHILD_ACCOUNT_ATTACHMENT = new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2));
    //TX 1
    public static final String TX_1_ID = "15791527287583198067";
    public static final String TX_1_SIGNATURE = "a0f0fb7062a89f9c5f09789a81fd27638c288085601e1d23d44824d2590f2905884446c90261378625fa7d1dafc3567f538bc5ce8d28da56d01d2bfdeb79ef02";
    public static final String SIGNED_TX_1_HEX = "0a102c010000a005d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a0f0fb7062a89f9c5f09789a81fd27638c288085601e1d23d44824d2590f2905884446c90261378625fa7d1dafc3567f538bc5ce8d28da56d01d2bfdeb79ef0200000000a0860100790000000000000001010200e45ccb4a81c412904e2f2ebb19f2f7cecc0ed81d279951503b5bbea377c7912092b29ad4a955e34aca052e590aac91d953b295d095b5a2a5523a2095c5ed7b67";
    public static final String SIGNED_TX_1_WRONG_LENGTH = "0a102c010000a005d52a07dc6fdf9f5c6b547ccb114ce7bba73a99014eb9ac647b6971bee9263c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a0f0fb7062a89f9c5f09789a81fd27638c288085601e1d23d44824d2590f2905884446c90261378625fa7d1dafc3567f538bc5ce8d28da56d01d2bfdeb79ef0200000000a0860100790000000000000001010200e45ccb4a81c412904e2f2ebb19f2f7cecc0ed81d279951503b5bbea377c7912092b29ad4a955e34aca052e590aac91d953b295d095b5a2a5523a2095c5ed7b67";
    public static final String SIGNED_TX_1_WRONG_HEX_FORMAT = "0a102c010O00a005d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a0f0fb7062a89f9c5f09789a81fd27638c288085601e1d23d44824d2590f2905884446c90261378625fa7d1dafc3567f538bc5ce8d28da56d01d2bfdeb79ef0200000000a0860100790000000000000001010200e45ccb4a81c412904e2f2ebb19f2f7cecc0ed81d279951503b5bbea377c7912092b29ad4a955e34aca052e590aac91d953b295d095b5a2a5523a2095c5ed7b67";
    public static final String UNSIGNED_TX_1_HEX = "0a102c010000a005d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a0860100790000000000000001010200e45ccb4a81c412904e2f2ebb19f2f7cecc0ed81d279951503b5bbea377c7912092b29ad4a955e34aca052e590aac91d953b295d095b5a2a5523a2095c5ed7b67";

}