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

package com.apollocurrency.aplwallet.apl;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Constants {
    //TODO: consider stamped lock or inner volatile static class to increase performance
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    public static Chain chain;
    public static boolean isTestnet = Apl.getBooleanProperty("apl.isTestnet");
    public static final boolean isOffline = Apl.getBooleanProperty("apl.isOffline");
    public static final boolean isLightClient = Apl.getBooleanProperty("apl.isLightClient");
    public static final String customLoginWarning = Apl.getStringProperty("apl.customLoginWarning", null, false, "UTF-8");

    public static String COIN_SYMBOL = "Apollo";
    public static String ACCOUNT_PREFIX = "APL";
    public static String PROJECT_NAME = "Apollo";
    private static int MAX_NUMBER_OF_TRANSACTIONS = Apl.getIntProperty("apl.maxNumberOfTransactions", 255);
    public static final int MIN_TRANSACTION_SIZE = 176;
    private static int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * MIN_TRANSACTION_SIZE;
    private static long MAX_BALANCE_APL = 30000000000L;
    public static final long ONE_APL = 100000000;
    private static long MAX_BALANCE_ATM = MAX_BALANCE_APL * ONE_APL;

    private static int BLOCK_TIME = 60;
    private static long INITIAL_BASE_TARGET = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(BLOCK_TIME * MAX_BALANCE_APL)).longValue();
    //153722867;
    private static long MAX_BASE_TARGET = INITIAL_BASE_TARGET * (isTestnet ? MAX_BALANCE_APL : 50);
    private static long MIN_BASE_TARGET = INITIAL_BASE_TARGET * 9 / 10;
    private static int MIN_BLOCKTIME_LIMIT = BLOCK_TIME - 7;
    private static int MAX_BLOCKTIME_LIMIT = BLOCK_TIME + 7;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final int MAX_ROLLBACK = Math.max(Apl.getIntProperty("apl.maxRollback"), 720);
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isTestnet ? Apl.getIntProperty("apl.testnetGuaranteedBalanceConfirmations", 1440) : 1440;
    public static int LEASING_DELAY = isTestnet ? Apl.getIntProperty("apl.testnetLeasingDelay", 1440) : 1440;
    public static final long MIN_FORGING_BALANCE_ATM = 1000 * ONE_APL;

    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
    public static final int FORGING_DELAY = Apl.getIntProperty("apl.forgingDelay");
    public static final int FORGING_SPEEDUP = Apl.getIntProperty("apl.forgingSpeedup");
    public static final int BATCH_COMMIT_SIZE = Apl.getIntProperty("apl.batchCommitSize", Integer.MAX_VALUE);
    public static final int TRIM_TRANSACTION_TIME_THRESHHOLD = Apl.getIntProperty("apl.trimOperationsLogThreshold", 1000);

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 160;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 160 + 16;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;

    public static int MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
    public static final int MAX_PRUNABLE_LIFETIME;
    public static final boolean ENABLE_PRUNING;
    static {
        int maxPrunableLifetime = Apl.getIntProperty("apl.maxPrunableLifetime");
        ENABLE_PRUNING = maxPrunableLifetime >= 0;
        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, MIN_PRUNABLE_LIFETIME) : Integer.MAX_VALUE;
    }
    public static final boolean INCLUDE_EXPIRED_PRUNABLE = Apl.getBooleanProperty("apl.includeExpiredPrunable");

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_ACCOUNT_PROPERTY_NAME_LENGTH = 32;
    public static final int MAX_ACCOUNT_PROPERTY_VALUE_LENGTH = 160;

    public static final long MAX_ASSET_QUANTITY_ATU = 30000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH = 160;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;
    public static final int MAX_DIVIDEND_PAYMENT_ROLLBACK = 1441;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;
    public static final int MAX_POLL_DURATION = 14 * 1440;

    public static final byte MIN_VOTE_VALUE = -92;
    public static final byte MAX_VOTE_VALUE = 92;
    public static final byte NO_VOTE_VALUE = Byte.MIN_VALUE;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 1000;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 30000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 30; // max possible at current block payload limit is 51
    public static final short MAX_SHUFFLING_REGISTRATION_PERIOD = (short)1440 * 7;
    public static short SHUFFLING_PROCESSING_DEADLINE = (short)(isTestnet ? 10 : 100);

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DATA_LENGTH = 42 * 1024;
//    Update
    public static final int UPDATE_URL_PART_LENGTH = 512;
    public static final int MAX_UPDATE_PLATFORM_LENGTH = 10;
    public static final int MAX_UPDATE_ARCHITECTURE_LENGTH = 10;
    public static final int MAX_UPDATE_VERSION_LENGTH = 10;
    public static final int MAX_UPDATE_HASH_LENGTH = 512;
    public static final int MIN_TOP_ACCOUNTS_NUMBER = 50;
    public static final int MAX_TOP_ACCOUNTS_NUMBER = 500;

    public static final int MAX_REFERENCED_TRANSACTION_TIMESPAN = 60 * 1440 * 60;
    public static final int CHECKSUM_BLOCK_1 = Integer.MAX_VALUE;

    public static final int LAST_CHECKSUM_BLOCK = 0;
    // LAST_KNOWN_BLOCK must also be set in html/www/js/nrs.constants.js
    public static int LAST_KNOWN_BLOCK = isTestnet ? 0 : 0;

    public static final Version MIN_VERSION = new Version(1, 0, 0);
    public static final Version MIN_PROXY_VERSION = new Version(1, 0, 0);

//    Testnet ports
    public static final int TESTNET_API_PORT = 6876;
    public static final int TESTNET_API_SSLPORT = 6877;
//    Peer ports
    public static final int DEFAULT_PEER_PORT = 47874;
    public static final int TESTNET_PEER_PORT = 46874;

    static long UNCONFIRMED_POOL_DEPOSIT_ATM = (isTestnet ? 50 : 100) * ONE_APL;
    public static long SHUFFLING_DEPOSIT_ATM = (isTestnet ? 7 : 1000) * ONE_APL;

    public static final boolean correctInvalidFees = Apl.getBooleanProperty("apl.correctInvalidFees");

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Constants() {} // never

    public static void init(Chain chain) {
        Constants.chain = chain;
        Constants.isTestnet = chain.isTestnet();
        Constants.PROJECT_NAME = chain.getProject();
        Constants.ACCOUNT_PREFIX = chain.getPrefix();
        Constants.COIN_SYMBOL = chain.getSymbol();
        Constants.LEASING_DELAY = isTestnet ? Apl.getIntProperty("apl.testnetLeasingDelay", 1440) : 1440;
        Constants.MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
        Constants.SHUFFLING_PROCESSING_DEADLINE = (short)(isTestnet ? 10 : 100);
        Constants.LAST_KNOWN_BLOCK = isTestnet ? 0 : 0;
        Constants.UNCONFIRMED_POOL_DEPOSIT_ATM = (isTestnet ? 50 : 100) * ONE_APL;
        Constants.SHUFFLING_DEPOSIT_ATM = (isTestnet ? 7 : 1000) * ONE_APL;
        updateConstants(chain.getBlockchainProperties().get(0));
    }


    static synchronized void updateConstants(BlockchainProperties bp) {
        LOCK.writeLock().lock();
        try {
            Constants.MAX_NUMBER_OF_TRANSACTIONS = bp.getMaxNumberOfTransactions();
            Constants.MAX_BALANCE_APL = bp.getMaxBalance();
            Constants.BLOCK_TIME = bp.getBlockTime();
            calculateConstants();
        } finally {
            LOCK.writeLock().unlock();
        }
    }
//
    static void updateToHeight() {
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        blockchainProperties.forEach((height, properties)-> {
            int currentHeight = Apl.getBlockchain().getHeight();
            if (currentHeight > height) {
                updateConstants(properties);
            }
        });
    }


    private static void calculateConstants() {
        Constants.MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * MIN_TRANSACTION_SIZE;
        Constants.MAX_BALANCE_ATM = MAX_BALANCE_APL * ONE_APL;
        Constants.INITIAL_BASE_TARGET = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(BLOCK_TIME * MAX_BALANCE_APL)).longValue();
        Constants.MAX_BASE_TARGET = INITIAL_BASE_TARGET * (isTestnet ? MAX_BALANCE_APL : 50);
        Constants.MIN_BASE_TARGET = INITIAL_BASE_TARGET * 9 / 10;
        Constants.MIN_BLOCKTIME_LIMIT = BLOCK_TIME - 7;
        Constants.MAX_BLOCKTIME_LIMIT = BLOCK_TIME + 7;
    }

    public static String getAccountPrefix() {
        LOCK.readLock().lock();
        try {
            return ACCOUNT_PREFIX;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static int getMaxNumberOfTransactions() {
        LOCK.readLock().lock();
        try {
            return MAX_NUMBER_OF_TRANSACTIONS;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static int getMaxPayloadLength() {
        LOCK.readLock().lock();
        try {
            return MAX_PAYLOAD_LENGTH;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static long getMaxBalanceAPL() {
        LOCK.readLock().lock();
        try {
            return MAX_BALANCE_APL;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static long getMaxBalanceATM() {
        LOCK.readLock().lock();
        try {
            return MAX_BALANCE_ATM;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static int getBlockTime() {
        LOCK.readLock().lock();
        try {
            return BLOCK_TIME;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static long getInitialBaseTarget() {
        LOCK.readLock().lock();
        try {
            return INITIAL_BASE_TARGET;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static long getMaxBaseTarget() {
        LOCK.readLock().lock();
        try {
            return MAX_BASE_TARGET;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static long getMinBaseTarget() {
        LOCK.readLock().lock();
        try {
            return MIN_BASE_TARGET;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static int getMinBlocktimeLimit() {
        LOCK.readLock().lock();
        try {
            return MIN_BLOCKTIME_LIMIT;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static int getMaxBlocktimeLimit() {
        LOCK.readLock().lock();
        try {
            return MAX_BLOCKTIME_LIMIT;
        } finally {
            LOCK.readLock().unlock();
        }
    }
}
