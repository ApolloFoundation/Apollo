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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.AplException;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.Time;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.slf4j.Logger;


public abstract class TransactionType {
    private static final Logger LOG = getLogger(TransactionType.class);

    public static final byte TYPE_PAYMENT = 0;
    public static final byte TYPE_MESSAGING = 1;
    public static final byte TYPE_COLORED_COINS = 2;
    public static final byte TYPE_DIGITAL_GOODS = 3;
    public static final byte TYPE_ACCOUNT_CONTROL = 4;
    public static final byte TYPE_MONETARY_SYSTEM = 5;
    public static final byte TYPE_DATA = 6;
    public static final byte TYPE_SHUFFLING = 7;
    public static final byte TYPE_UPDATE = 8;

    public static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
    public static final byte SUBTYPE_PAYMENT_PRIVATE_PAYMENT = 1;

    public static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    public static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    public static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    public static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    public static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    public static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    public static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    public static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    public static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY = 10;
    public static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE = 11;

    public static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    public static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    public static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    public static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    public static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    public static final byte SUBTYPE_COLORED_COINS_ASSET_DELETE = 7;

    public static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    public static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    public static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    public static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    public static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    public static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    public static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    public static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    public static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    public static final byte SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY = 1;

    public static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    public static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;

    public static final byte SUBTYPE_UPDATE_CRITICAL = 0;
    public static final byte SUBTYPE_UPDATE_IMPORTANT = 1;
    public static final byte SUBTYPE_UPDATE_MINOR = 2;

    public static final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    protected static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    public static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    case SUBTYPE_PAYMENT_PRIVATE_PAYMENT:
                    	return Payment.PRIVATE;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        throw new IllegalArgumentException("Hub Announcement no longer supported");
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    case SUBTYPE_MESSAGING_ALIAS_DELETE:
                        return Messaging.ALIAS_DELETE;
                    case SUBTYPE_MESSAGING_PHASING_VOTE_CASTING:
                        return Messaging.PHASING_VOTE_CASTING;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY:
                        return Messaging.ACCOUNT_PROPERTY;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE:
                        return Messaging.ACCOUNT_PROPERTY_DELETE;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoins.BID_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT:
                        return ColoredCoins.DIVIDEND_PAYMENT;
                    case SUBTYPE_COLORED_COINS_ASSET_DELETE:
                        return ColoredCoins.ASSET_DELETE;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return AccountControl.EFFECTIVE_BALANCE_LEASING;
                    case SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY:
                        return AccountControl.SET_PHASING_ONLY;
                    default:
                        return null;
                }
            case TYPE_MONETARY_SYSTEM:
                return MonetarySystem.findTransactionType(subtype);
            case TYPE_DATA:
                switch (subtype) {
                    case SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                        return Data.TAGGED_DATA_UPLOAD;
                    case SUBTYPE_DATA_TAGGED_DATA_EXTEND:
                        return Data.TAGGED_DATA_EXTEND;
                    default:
                        return null;
                }
            case TYPE_SHUFFLING:
                return ShufflingTransaction.findTransactionType(subtype);
            case TYPE_UPDATE:
                switch (subtype) {
                    case SUBTYPE_UPDATE_CRITICAL:
                        return Update.CRITICAL;
                    case SUBTYPE_UPDATE_IMPORTANT:
                        return Update.IMPORTANT;
                    case SUBTYPE_UPDATE_MINOR:
                        return Update.MINOR;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }


    public TransactionType() {}

    public abstract byte getType();

    public abstract byte getSubtype();

    public abstract LedgerEvent getLedgerEvent();

    public abstract AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException;

    public abstract AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException;

    public abstract void validateAttachment(Transaction transaction) throws AplException.ValidationException;

    // return false if double spending
    public final boolean applyUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        long amountATM = transaction.getAmountATM();
        long feeATM = transaction.getFeeATM();
        if (transaction.referencedTransactionFullHash() != null) {
            feeATM = Math.addExact(feeATM, blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        if (senderAccount.getUnconfirmedBalanceATM() < totalAmountATM) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -amountATM, -feeATM);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), amountATM, feeATM);
            return false;
        }
        return true;
    }

    public abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public void apply(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
        long amount = transaction.getAmountATM();
        long transactionId = transaction.getId();
        if (!transaction.attachmentIsPhased()) {
            senderAccount.addToBalanceATM(getLedgerEvent(), transactionId, -amount, -transaction.getFeeATM());
        } else {
            senderAccount.addToBalanceATM(getLedgerEvent(), transactionId, -amount);
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceATM(getLedgerEvent(), transactionId, amount);
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    public abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    public final void undoUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                transaction.getAmountATM(), transaction.getFeeATM());
        if (transaction.referencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), 0,
                    blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
    }

    public abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    // isBlockDuplicate and isDuplicate share the same duplicates map, but isBlockDuplicate check is done first
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    public static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, boolean exclusive) {
        return isDuplicate(uniqueType, key, duplicates, exclusive ? 0 : Integer.MAX_VALUE);
    }

    public static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, int maxCount) {
        Map<String,Integer> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashMap<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        Integer currentCount = typeDuplicates.get(key);
        if (currentCount == null) {
            typeDuplicates.put(key, maxCount > 0 ? 1 : 0);
            return false;
        }
        if (currentCount == 0) {
            return true;
        }
        if (currentCount < maxCount) {
            typeDuplicates.put(key, currentCount + 1);
            return false;
        }
        return true;
    }

    public boolean isPruned(long transactionId) {
        return false;
    }

    public abstract boolean canHaveRecipient();

    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    public abstract boolean isPhasingSafe();

    public boolean isPhasable() {
        return true;
    }

    public Fee getBaselineFee(Transaction transaction) {
        return Fee.DEFAULT_FEE;
    }

    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    public int getBaselineFeeHeight() {
        return 1;
    }

    public int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    public long[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_LONG;
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " type: " + getType() + ", subtype: " + getSubtype();
    }

}
