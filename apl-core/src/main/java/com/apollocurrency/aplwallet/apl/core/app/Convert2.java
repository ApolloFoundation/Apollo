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
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import java.util.Objects;


public final class Convert2 {

    private static final String BLOCKCHAIN_IS_NULL_MSG = "Blockchain config is null";
    private static BlockchainConfig blockchainConfig;
    private static boolean initialized = false;

    private Convert2() {
    } //never

    public static void init(BlockchainConfig bcConfig) {
        Objects.requireNonNull(bcConfig, BLOCKCHAIN_IS_NULL_MSG);
        blockchainConfig = bcConfig;
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static void validate() {
        if (!isInitialized()) {
            throw new IllegalStateException(BLOCKCHAIN_IS_NULL_MSG);
        }
    }

    //TODO: rewrite other classes without defaultRsAccount
    public static String rsAccount(long accountId) {
        validate();
        return blockchainConfig.getAccountPrefix() + "-" + Crypto.rsEncode(accountId);
    }

    public static String rsAccount(String accountPrefix, long accountId) {
        return accountPrefix + "-" + Crypto.rsEncode(accountId);
    }

    //avoid static initialization chain when call Constants.ACCOUNT_PREFIX in rsAccount method
    public static String defaultRsAccount(long accountId) {
        return "APL-" + Crypto.rsEncode(accountId);
    }


    public static long fromEpochTime(int epochTime) {
        return epochTime * 1000L + GenesisImporter.EPOCH_BEGINNING - 500L;
    }

    /**
     * Time after genesis block.
     *
     * @param currentTime (milliseconds)
     * @return seconds
     */
    public static int toEpochTime(long currentTime) {
        return (int) ((currentTime - GenesisImporter.EPOCH_BEGINNING + 500) / 1000);
    }

}
