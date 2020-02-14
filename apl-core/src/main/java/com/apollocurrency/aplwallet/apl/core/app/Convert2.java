/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import javax.enterprise.inject.spi.CDI;


public final class Convert2 {

    private Convert2() {} //never

    //TODO: rewrite other classes without defaultRsAccount
    public static String rsAccount(long accountId) {
        return CDI.current().select(BlockchainConfig.class).get().getAccountPrefix() + "-" + Crypto.rsEncode(accountId);
    }

    public static String rsAccount(String accountPrefix, long accountId) {
        return accountPrefix + "-" + Crypto.rsEncode(accountId);
    }
    //avoid static initialization chain when call Constants.ACCOUNT_PREFIX in rsAccount method
    public static String defaultRsAccount(long accountId) {
        return  "APL-" + Crypto.rsEncode(accountId);
    }


    public static long fromEpochTime(int epochTime) {
        return epochTime * 1000L + GenesisImporter.EPOCH_BEGINNING - 500L;
    }

    /**
     * Time after genesis block.
     * @param currentTime (milliseconds)
     * @return seconds
     */
    public static int toEpochTime(long currentTime) {
        return (int)((currentTime - GenesisImporter.EPOCH_BEGINNING + 500) / 1000);
    }

}
