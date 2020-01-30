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
package com.apollocurrency.aplwallet.apl.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Constants {

    public static final Version VERSION = new Version("1.41.9");

    public static final String APPLICATION = "Apollo";
    public static final String APPLICATION_DIR_NAME = "apl-blockchain";
    public static final String DESKTOP_APPLICATION_NAME = "apl-desktop";
    public static final int DEFAULT_TRIM_FREQUENCY = 1000;
    public static final long LONG_TIME_TWO_SECONDS = 2000L;
    public static final long LONG_TIME_FIVE_SECONDS = 5000L;
    public static final int MAX_AUTO_ROLLBACK = 720; //number of blocks that forms fork

    public static final long ONE_APL = 100000000;
    public static final long APL_COMMISSION = 10;
    public static final long ETH_GAS_MULTIPLIER = 5;

    public static final int OFFER_VALIDATE_OK = 1;
    public static final int OFFER_VALIDATE_ERROR_APL_FREEZE = -1;
    public static final int OFFER_VALIDATE_ERROR_APL_COMMISSION = -2;
    public static final int OFFER_VALIDATE_ERROR_ETH_COMMISSION = -3;
    public static final int OFFER_VALIDATE_ERROR_ETH_DEPOSIT = -4;
    public static final int OFFER_VALIDATE_ERROR_IN_PARAMETER = -5;
    public static final int OFFER_VALIDATE_ERROR_ETH_SYSTEM = -6;
    public static final int OFFER_VALIDATE_ERROR_ATOMIC_SWAP_IS_NOT_EXIST = -7;
    public static final int OFFER_VALIDATE_ERROR_PHASING_IS_NOT_EXIST = -8;
    public static final int OFFER_VALIDATE_ERROR_PHASING_WAS_FINISHED = -9;
    public static final int OFFER_VALIDATE_ERROR_TIME_IS_NOT_CORRECT = -10;
    public static final int OFFER_VALIDATE_ERROR_APL_DEPOSIT = -11;
    public static final int OFFER_VALIDATE_ERROR_UNKNOWN = -99;

    public static final int ONE_DAY_SECS = 24 * 3600;

    public static final int MIN_TRANSACTION_SIZE = 176;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final long MIN_FORGING_BALANCE_ATM = 1000 * ONE_APL;

    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;

    // 2 days.
    public static final int MAX_PHASING_TIME_DURATION_SEC = 2 * 24 * 60 * 60;
    // 24 hours
    public static final int MAX_ORDER_DURATION_SEC = 24 * 60 * 60;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 160;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 160 + 16;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;
    public static final int DEFAULT_PRUNABLE_UPDATE_PERIOD = 3600;
    // 5 min
    public static final int PRUNABLE_MONITOR_INITIAL_DELAY = 5 * 60 * 1000;
    // 10 min
    public static final int PRUNABLE_MONITOR_DELAY = 10 * 60 * 1000;

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

    public static final int MAX_DGS_LISTING_QUANTITY = 1_000_000_000;
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
    public static final short MAX_SHUFFLING_REGISTRATION_PERIOD = (short) 1440 * 7;

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

    public static final Version MIN_VERSION = new Version(1, 0, 0);
    public static final Version MIN_PROXY_VERSION = new Version(1, 0, 0);

    public static final int DEFAULT_PEER_PORT = 47874;
    public static final int PEER_RECONNECT_ATTMEPT_DELAY = 60; //now 1 min, was 600 or 10 min
    /**
     * blacklist on 1/10 of this number and forget peer if it is can not be
     * connected such number of times
     */
    public static final int PEER_RECONNECT_ATTMEPTS_MAX = 80;
    public static final int PEER_UPDATE_INTERVAL = 1800; //now 30 min, was 3600, one hour

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    //Eth/Pax
    public static final BigInteger GAS_LIMIT_ETHER_TX = BigInteger.valueOf(21_000);
    public static final BigInteger GAS_LIMIT_FOR_ERC20 = BigInteger.valueOf(300_000);
    public static final BigInteger GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT = BigInteger.valueOf(400_000);
    public static final Integer MAX_ADDRESS_LENGTH = 110;

    public static String ETH_DEFAULT_ADDRESS = "0x0000000000000000000000000000000000000000";

    //DEX
    public static String ETH_STATION_GAS_INFO_URL = "https://www.ethgasstation.info/json/ethgasAPI.json";
    public static String ETH_CHAIN_GAS_INFO_URL = "https://www.etherchain.org/api/gasPriceOracle";
    public static String ETH_GAS_INFO_URL = "https://ethgasstation.info/json/ethgasAPI.json";

    public static BigInteger ETH_MAX_POS_INT = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    //TODO calculate this value on the future.
    public static BigDecimal DEX_MIN_ETH_FEE = BigDecimal.valueOf(0.002);

    //168h
    public static final int DEX_MAX_CONTRACT_TIME_WAITING_TO_REPLY = 7 * 24 * 60 * 60;

    public static final int DEX_NUMBER_OF_PENDING_ORDER_CONFIRMATIONS = 1000;

    public static final int DEX_GRAPH_INTERVAL_MIN = 60;
    public static final int DEX_GRAPH_INTERVAL_HOUR = 60 * 60;
    public static final int DEX_GRAPH_INTERVAL_DAY = 60 * 60 * 24;

    private Constants() {
    } // never

}
