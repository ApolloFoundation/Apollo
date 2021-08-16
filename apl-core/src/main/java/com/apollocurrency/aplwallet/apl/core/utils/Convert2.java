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

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
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
        if (blockchainConfig == null) {
            String error = "blockchainConfig should be initialized explicitly first, see Convert2.init(...)";
            log.error(error);
            throw new RuntimeException(error);
        }
        return blockchainConfig.getAccountPrefix() + "-" + Crypto.rsEncode(accountId);
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

    /**
     * Ensure, that multiplication of the two given values, x and y, will not exceed {@link Long} java type limits and return result
     * of that multiplication, otherwise throw exception with details appended by the given message
     * @param x first value to multiply
     * @param y second value to multiply by first value
     * @param message error message, which will be attached to an exception in a case of failed multiplication
     * @return multiplication result
     * @throws AplException.NotValidException when multiplication result exceeds {@link Long} java type limits
     */
    public static long safeMultiply(long x, long y, String message) throws AplException.NotValidException {
        try {
            return Math.multiplyExact(x, y);
        } catch (ArithmeticException e) {
            throw new AplException.NotValidException("Result of multiplying x=" + x + ", y=" + y + " exceeds the allowed range [" + Long.MIN_VALUE + ";"  + Long.MAX_VALUE + "], " + message, e);
        }
    }

    /**
     * Do the same as {@link Convert2#safeMultiply(long, long, String)} but construct error message from the given transaction object
     * @param x same as for {@link Convert2#safeMultiply(long, long, String)}
     * @param y same as for {@link Convert2#safeMultiply(long, long, String)}
     * @param tx transaction, for which error message will be constructed using transaction id, sender's id and transaction type spec
     * @return same as {@link Convert2#safeMultiply(long, long, String)}
     * @throws AplException.NotValidException when {@link Convert2#safeMultiply(long, long, String)} throws
     */
    public static long safeMultiply(long x, long y, Transaction tx) throws AplException.NotValidException {
        return safeMultiply(x, y, "transaction=" + tx.getStringId() + ", type=" + tx.getType().getSpec() + ", sender=" + Long.toUnsignedString(tx.getSenderId()));
    }

}
