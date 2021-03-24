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
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public final class Convert2 {
    private static final String ACCOUNT_PREFIX_IS_NULL_MSG = "AccountPrefix is null";
    private static final String EPOCH_BEGINNING_IS_NULL_MSG = "EpochBeginning is null";
    private volatile static String accountPrefix;
    private volatile static Long epochBeginning;
    private volatile static boolean initialized = false;

    Convert2() {
    }

    //TODO move to property
    public static void init(String prefix, long epochBeg) {
        Objects.requireNonNull(prefix, ACCOUNT_PREFIX_IS_NULL_MSG);
        Objects.requireNonNull(epochBeg, EPOCH_BEGINNING_IS_NULL_MSG);
        accountPrefix = prefix;
        epochBeginning = epochBeg;
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static void validate() {
        if (!isInitialized()) {
            throw new IllegalStateException(ACCOUNT_PREFIX_IS_NULL_MSG);
        }
    }

    //TODO: rewrite other classes without defaultRsAccount
    public static String rsAccount(long accountId) {
        validate();
        return accountPrefix + "-" + Crypto.rsEncode(accountId);
    }

    public static String rsAccount(String accountPrefix, long accountId) {
        return accountPrefix + "-" + Crypto.rsEncode(accountId);
    }

    //avoid static initialization chain when call Constants.ACCOUNT_PREFIX in rsAccount method
    public static String defaultRsAccount(long accountId) {
        if (accountPrefix == null) {
            String error = "blockchainConfig should be initialized explicitly first, see Convert2.init(...)";
            log.error(error);
            throw new RuntimeException(error);
        }
        return accountPrefix + "-" + Crypto.rsEncode(accountId);
    }


    public static long fromEpochTime(int epochTime) {
        return epochTime * 1000L + epochBeginning - 500L;
    }

    /**
     * Time after genesis block.
     *
     * @param currentTime (milliseconds)
     * @return seconds
     */
    public static int toEpochTime(long currentTime) {
        return (int) ((currentTime - epochBeginning + 500) / 1000);
    }

}
