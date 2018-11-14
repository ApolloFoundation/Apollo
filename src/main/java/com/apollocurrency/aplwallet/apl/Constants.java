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

import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.chainid.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.chainid.Chain;
import com.apollocurrency.aplwallet.apl.chainid.Consensus;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

public final class Constants {
    private static final Logger LOG = getLogger(Constants.class);

    private static Chain chain;
    private static boolean testnet;
    public static final boolean isOffline = Apl.getBooleanProperty("apl.isOffline");
    public static final boolean isLightClient = Apl.getBooleanProperty("apl.isLightClient");
    public static final String customLoginWarning = Apl.getStringProperty("apl.customLoginWarning", null, false, "UTF-8");
    private volatile static ChangeableConstants changeableConstants;
    private static class ChangeableConstants {
        public final int maxNumberOfTransactions;
        public final int maxPayloadLength;
        public final long maxBalanceApl;
        public final long maxBalanceAtm;
        public final int blockTime;
        public final long initialBaseTarget;
        public final long maxBaseTarget;
        public final long minBaseTarget;
        public final int minBlocktimeLimit;
        public final int maxBlocktimeLimit;
        public final boolean isAdaptiveForgingEnabled;
        public final int adaptiveForgingEmptyBlockTime;
        public final Consensus.Type consensusType;

        private ChangeableConstants(BlockchainProperties bp) {
            this.maxNumberOfTransactions = bp.getMaxNumberOfTransactions();
            this.maxBalanceApl = bp.getMaxBalance();
            this.blockTime = bp.getBlockTime();
            this.maxPayloadLength = maxNumberOfTransactions * MIN_TRANSACTION_SIZE;
            this.maxBalanceAtm = maxBalanceApl * ONE_APL;
            this.initialBaseTarget = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(blockTime * maxBalanceApl)).longValue();
            this.maxBaseTarget = initialBaseTarget * (testnet ? maxBalanceApl : 50);
            this.minBaseTarget = initialBaseTarget * 9 / 10;
            this.minBlocktimeLimit = bp.getMinBlockTimeLimit();
            this.maxBlocktimeLimit = bp.getMaxBlockTimeLimit();
            this.isAdaptiveForgingEnabled = bp.getConsensus().getAdaptiveForgingSettings().isEnabled();
            this.adaptiveForgingEmptyBlockTime = bp.getConsensus().getAdaptiveForgingSettings().getEmptyBlockTime();
            this.consensusType = bp.getConsensus().getType();
        }

        @Override
        public String toString() {
            return "ChangeableConstants{" +
                    "maxNumberOfTransactions=" + maxNumberOfTransactions +
                    ", maxPayloadLength=" + maxPayloadLength +
                    ", maxBalanceApl=" + maxBalanceApl +
                    ", maxBalanceAtm=" + maxBalanceAtm +
                    ", blockTime=" + blockTime +
                    ", initialBaseTarget=" + initialBaseTarget +
                    ", maxBaseTarget=" + maxBaseTarget +
                    ", minBaseTarget=" + minBaseTarget +
                    ", minBlocktimeLimit=" + minBlocktimeLimit +
                    ", maxBlocktimeLimit=" + maxBlocktimeLimit +
                    ", isAdaptiveForgingEnabled=" + isAdaptiveForgingEnabled +
                    ", adaptiveForgingEmptyBlockTime=" + adaptiveForgingEmptyBlockTime +
                    ", consensusType=" + consensusType +
                    '}';
        }
    }
    private static String coinSymbol;
    private static String accountPrefix;
    private static String projectName;

    public static final long ONE_APL = 100000000;

    public static final int MIN_TRANSACTION_SIZE = 176;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final int MAX_ROLLBACK = Math.max(Apl.getIntProperty("apl.maxRollback"), 720);
    private static int guaranteedBalanceConfirmations;
    private static int leasingDelay = testnet ? Apl.getIntProperty("apl.testnetLeasingDelay", 1440) : 1440;
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

    private static int minPrunableLifetime;
    public static final int MAX_PRUNABLE_LIFETIME;
    public static final boolean ENABLE_PRUNING;
    static {
        int maxPrunableLifetime = Apl.getIntProperty("apl.maxPrunableLifetime");
        ENABLE_PRUNING = maxPrunableLifetime >= 0;
        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, minPrunableLifetime) : Integer.MAX_VALUE;
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
    private static short shufflingProcessingDeadline;

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
    // lastKnownBlock must also be set in html/www/js/ars.constants.js
    private static int lastKnownBlock;

    public static final Version MIN_VERSION = new Version(1, 0, 0);
    public static final Version MIN_PROXY_VERSION = new Version(1, 0, 0);

//    Testnet ports
    public static final int TESTNET_API_PORT = 6876;
    public static final int TESTNET_API_SSLPORT = 6877;
//    Peer ports
    public static final int DEFAULT_PEER_PORT = 47874;
    public static final int TESTNET_PEER_PORT = 46874;

    private static long unconfirmedPoolDepositAtm;
    private static long shufflingDepositAtm;

    public static final boolean correctInvalidFees = Apl.getBooleanProperty("apl.correctInvalidFees");

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String TIME_SERVICE = "pool.ntp.org";
    
    private Constants() {} // never

    public static void init(Chain chain) {
        Constants.chain = chain;
        Constants.testnet = chain.isTestnet();
        Constants.projectName = chain.getProject();
        Constants.accountPrefix = chain.getPrefix();
        Constants.coinSymbol = chain.getSymbol();
        Constants.leasingDelay = testnet ? Apl.getIntProperty("apl.testnetLeasingDelay", 1440) : 1440;
        Constants.minPrunableLifetime = testnet ? 1440 * 60 : 14 * 1440 * 60;
        Constants.shufflingProcessingDeadline = (short)(testnet ? 10 : 100);
        Constants.lastKnownBlock = testnet ? 0 : 0;
        Constants.unconfirmedPoolDepositAtm = (testnet ? 50 : 100) * ONE_APL;
        Constants.shufflingDepositAtm = (testnet ? 7 : 1000) * ONE_APL;
        Constants.guaranteedBalanceConfirmations = testnet ? Apl.getIntProperty("apl.testnetGuaranteedBalanceConfirmations", 1440) : 1440;
        changeableConstants = new ChangeableConstants(chain.getBlockchainProperties().get(0));
        ConstantsChangeListener constantsChangeListener = new ConstantsChangeListener(chain.getBlockchainProperties());
        BlockchainProcessorImpl.getInstance().addListener(constantsChangeListener,
                BlockchainProcessor.Event.BLOCK_PUSHED);
        BlockchainProcessorImpl.getInstance().addListener(constantsChangeListener,
                BlockchainProcessor.Event.BLOCK_POPPED);
        BlockchainProcessorImpl.getInstance().addListener(constantsChangeListener,
                BlockchainProcessor.Event.BLOCK_SCANNED);
        LOG.debug("Connected to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
    }

    private static ChangeableConstants getConstantsAtHeight(Chain chain, int targetHeight, boolean inclusive) {
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (targetHeight == 0) {
            return new ChangeableConstants(blockchainProperties.get(0));
        }
        Optional<Integer> maxHeight =
                blockchainProperties
                        .keySet()
                        .stream()
                        .filter(height -> inclusive ? targetHeight >= height : targetHeight > height)
                        .max(Comparator.naturalOrder());
        return maxHeight
                .map(height -> new ChangeableConstants(blockchainProperties.get(height)))
                .orElse(null);
    }

    public static int getGuaranteedBalanceConfirmations() {
        return guaranteedBalanceConfirmations;
    }

    static void updateToLatestConstants() {
        BlockImpl lastBlock = BlockDb.findLastBlock();
        if (lastBlock == null) {
            LOG.debug("Nothing to update. No blocks");
            return;
        }
        updateToHeight(lastBlock.getHeight(), true);
    }
    private static void updateToHeight(int height, boolean inclusive) {
        Objects.requireNonNull(chain);

        ChangeableConstants latestConstants = getConstantsAtHeight(chain, height, inclusive);
        if (latestConstants != null) {
            changeableConstants = latestConstants;
        } else {
            LOG.error("No constants at all!");
        }
    }

    public static Chain getChain() {
        return chain;
    }

    public static int getMaxNumberOfTransactions() {
        return changeableConstants.maxNumberOfTransactions;
    }


    public static int getMaxPayloadLength() {
        return changeableConstants.maxPayloadLength;
    }

    public static long getMaxBalanceAPL() {
        return changeableConstants.maxBalanceApl;
    }

    public static long getMaxBalanceATM() {
        return changeableConstants.maxBalanceAtm;
    }

    public static int getBlockTime() {
        return changeableConstants.blockTime;
    }

    public static long getInitialBaseTarget() {
        return changeableConstants.initialBaseTarget;
    }

    public static long getMaxBaseTarget() {
        return changeableConstants.maxBaseTarget;
    }

    public static long getMinBaseTarget() {
        return changeableConstants.minBaseTarget;
    }

    public static int getMinBlocktimeLimit() {
        return changeableConstants.minBlocktimeLimit;
    }

    public static int getMaxBlocktimeLimit() {
        return changeableConstants.maxBlocktimeLimit;
    }

    public static int getAdaptiveForgingEmptyBlockTime() {
        return changeableConstants.adaptiveForgingEmptyBlockTime;
    }

    public static boolean isAdaptiveForgingEnabled() {
        return changeableConstants.isAdaptiveForgingEnabled;
    }

    public static Consensus.Type getConsesusType() {
        return changeableConstants.consensusType;
    }

    // Protect non-final constants from modification
    public static boolean isTestnet() {
        return testnet;
    }

    public static String getCoinSymbol() {
        return coinSymbol;
    }

    public static String getAccountPrefix() {
        return accountPrefix;
    }

    public static String getProjectName() {
        return projectName;
    }

    public static int getLeasingDelay() {
        return leasingDelay;
    }

    public static int getMinPrunableLifetime() {
        return minPrunableLifetime;
    }

    public static short getShufflingProcessingDeadline() {
        return shufflingProcessingDeadline;
    }

    public static int getLastKnownBlock() {
        return lastKnownBlock;
    }

    public static long getUnconfirmedPoolDepositAtm() {
        return unconfirmedPoolDepositAtm;
    }

    public static long getShufflingDepositAtm() {
        return shufflingDepositAtm;
    }

    private static class ConstantsChangeListener implements Listener<Block> {
        private static final Logger LOG = getLogger(ConstantsChangeListener.class);
        private final Map<Integer, BlockchainProperties> propertiesMap;
        private final Set<Integer> targetHeights;

        public ConstantsChangeListener(Map<Integer, BlockchainProperties> propertiesMap) {
            this.propertiesMap = new ConcurrentHashMap<>(propertiesMap);
            this.targetHeights = Collections.unmodifiableSet(propertiesMap.keySet());
            String stringConstantsChangeHeights =
                    targetHeights.stream().map(Object::toString).collect(Collectors.joining(
                    ","));
            LOG.debug("Constants updates at heights: {}",
                    stringConstantsChangeHeights.isEmpty() ? "none" : stringConstantsChangeHeights);
        }

        @Override
        public void notify(Block block) {
            int currentHeight = block.getHeight();
            if (targetHeights.contains(currentHeight)) {
                LOG.info("Updating constants at height {}", currentHeight);
                changeableConstants = new ChangeableConstants(propertiesMap.get(currentHeight));
                LOG.info("New constants applied: {}", changeableConstants);
            }
        }
    }

    public static void rollback(int height) {
        Constants.updateToHeight(height, true);
    }

    public static boolean isAdaptiveBlockAtHeight(int height) {
        ChangeableConstants constantsAtHeight = getConstantsAtHeight(chain, height, false);
        return constantsAtHeight.isAdaptiveForgingEnabled;
    }
}
