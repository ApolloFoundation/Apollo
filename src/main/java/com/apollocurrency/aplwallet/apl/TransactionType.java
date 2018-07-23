/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.Account.ControlType;
import com.apollocurrency.aplwallet.apl.AccountLedger.LedgerEvent;
import com.apollocurrency.aplwallet.apl.AplException.ValidationException;
import com.apollocurrency.aplwallet.apl.Attachment.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.VoteWeighting.VotingModel;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    static final byte TYPE_MONETARY_SYSTEM = 5;
    private static final byte TYPE_DATA = 6;
    static final byte TYPE_SHUFFLING = 7;
    private static final byte TYPE_UPDATE = 8;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
    private static final byte SUBTYPE_PAYMENT_PRIVATE_PAYMENT = 1;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    private static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    private static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    private static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    private static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY = 10;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE = 11;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    private static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_DELETE = 7;

    private static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    private static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    private static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    private static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    private static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    private static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    private static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    private static final byte SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY = 1;

    private static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    private static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;

    private static final byte SUBTYPE_UPDATE_CRITICAL = 0;
    private static final byte SUBTYPE_UPDATE_IMPORTANT = 1;
    private static final byte SUBTYPE_UPDATE_MINOR = 2;

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
                        return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
                    case SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY:
                        return TransactionType.AccountControl.SET_PHASING_ONLY;
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


    TransactionType() {}

    public abstract byte getType();

    public abstract byte getSubtype();

    public abstract LedgerEvent getLedgerEvent();

    abstract Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException;

    abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException;

    abstract void validateAttachment(Transaction transaction) throws AplException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        long amountATM = transaction.getAmountATM();
        long feeATM = transaction.getFeeATM();
        if (transaction.referencedTransactionFullHash() != null) {
            feeATM = Math.addExact(feeATM, Constants.UNCONFIRMED_POOL_DEPOSIT_ATM);
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

    abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
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

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                transaction.getAmountATM(), transaction.getFeeATM());
        if (transaction.referencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), 0,
                    Constants.UNCONFIRMED_POOL_DEPOSIT_ATM);
        }
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    // isBlockDuplicate and isDuplicate share the same duplicates map, but isBlockDuplicate check is done first
    boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, boolean exclusive) {
        return isDuplicate(uniqueType, key, duplicates, exclusive ? 0 : Integer.MAX_VALUE);
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, int maxCount) {
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

    boolean isPruned(long transactionId) {
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

    Fee getBaselineFee(Transaction transaction) {
        return Fee.DEFAULT_FEE;
    }

    Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    int getBaselineFeeHeight() {
        return 1;
    }

    int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    long[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_LONG;
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " type: " + getType() + ", subtype: " + getSubtype();
    }

    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            if (recipientAccount == null) {
                Account.getAccount(Genesis.CREATOR_ID).addToBalanceAndUnconfirmedBalanceATM(getLedgerEvent(),
                        transaction.getId(), transaction.getAmountATM());
            }
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return true;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            public final LedgerEvent getLedgerEvent() {
                return LedgerEvent.ORDINARY_PAYMENT;
            }

            @Override
            public String getName() {
                return "OrdinaryPayment";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= Constants.MAX_BALANCE_ATM) {
                    throw new AplException.NotValidException("Invalid ordinary payment");
                }
            }

        };

        public static final TransactionType PRIVATE = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_PRIVATE_PAYMENT;
            }

            @Override
            public final LedgerEvent getLedgerEvent() {
                return LedgerEvent.PRIVATE_PAYMENT;
            }

            @Override
            public String getName() {
                return "PrivatePayment";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return Attachment.PRIVATE_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return Attachment.PRIVATE_PAYMENT;
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= Constants.MAX_BALANCE_ATM) {
                    throw new AplException.NotValidException("Invalid private payment");
                }
            }
        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ARBITRARY_MESSAGE;
            }

            @Override
            public String getName() {
                return "ArbitraryMessage";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountATM() != 0) {
                    throw new AplException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Sending messages to Genesis not allowed.");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            private final Fee ALIAS_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                    return attachment.getAliasName().length() + attachment.getAliasURI().length();
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ALIAS_ASSIGNMENT;
            }

            @Override
            public String getName() {
                return "AliasAssignment";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ALIAS_FEE;
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAliasAssignment(buffer);
            }

            @Override
            Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAliasAssignment(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias.addOrUpdateAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return Alias.getAlias(((Attachment.MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null
                        && isDuplicate(Messaging.ALIAS_ASSIGNMENT, "", duplicates, true);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new AplException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new AplException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ALIAS_SELL;
            }
            @Override
            public String getName() {
                return "AliasSell";
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAliasSell(buffer);
            }

            @Override
            Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAliasSell(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                Alias.sellAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                if (transaction.getAmountATM() != 0) {
                    throw new AplException.NotValidException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new AplException.NotValidException("Missing alias name");
                }
                long priceATM = attachment.getPriceATM();
                if (priceATM < 0 || priceATM > Constants.MAX_BALANCE_ATM) {
                    throw new AplException.NotValidException("Invalid alias sell price: " + priceATM);
                }
                if (priceATM == 0) {
                    if (Genesis.CREATOR_ID == transaction.getRecipientId()) {
                        throw new AplException.NotValidException("Transferring aliases to Genesis account not allowed");
                    } else if (transaction.getRecipientId() == 0) {
                        throw new AplException.NotValidException("Missing alias transfer recipient");
                    }
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Selling alias to Genesis not allowed");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ALIAS_BUY;
            }

            @Override
            public String getName() {
                return "AliasBuy";
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAliasBuy(buffer);
            }

            @Override
            Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAliasBuy(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                Alias.changeOwner(transaction.getSenderId(), aliasName);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getRecipientId()) {
                    throw new AplException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
                            + Long.toUnsignedString(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(alias);
                if (offer == null) {
                    throw new AplException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountATM() < offer.getPriceATM()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountATM() + " < " + offer.getPriceATM() + ")";
                    throw new AplException.NotCurrentlyValidException(msg);
                }
                if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
                    throw new AplException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
                            + Long.toUnsignedString(transaction.getSenderId()) + " expected: "
                            + Long.toUnsignedString(offer.getBuyerId()));
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_DELETE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_DELETE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ALIAS_DELETE;
            }

            @Override
            public String getName() {
                return "AliasDelete";
            }

            @Override
            Attachment.MessagingAliasDelete parseAttachment(final ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAliasDelete(buffer);
            }

            @Override
            Attachment.MessagingAliasDelete parseAttachment(final JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAliasDelete(attachmentData);
            }

            @Override
            void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                Alias.deleteAlias(attachment.getAliasName());
            }

            @Override
            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasDelete attachment = (Attachment.MessagingAliasDelete) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            void validateAttachment(final Transaction transaction) throws AplException.ValidationException {
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new AplException.NotValidException("Missing alias name");
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {

            private final Fee POLL_OPTIONS_FEE = new Fee.SizeBasedFee(10 * Constants.ONE_APL, Constants.ONE_APL, 1) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    int numOptions = ((Attachment.MessagingPollCreation)appendage).getPollOptions().length;
                    return numOptions <= 19 ? 0 : numOptions - 19;
                }
            };

            private final Fee POLL_SIZE_FEE = new Fee.SizeBasedFee(0, 2 * Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)appendage;
                    int size = attachment.getPollName().length() + attachment.getPollDescription().length();
                    for (String option : ((Attachment.MessagingPollCreation)appendage).getPollOptions()) {
                        size += option.length();
                    }
                    return size <= 288 ? 0 : size - 288;
                }
            };

            private final Fee POLL_FEE = (transaction, appendage) ->
                    POLL_OPTIONS_FEE.getFee(transaction, appendage) + POLL_SIZE_FEE.getFee(transaction, appendage);

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.POLL_CREATION;
            }

            @Override
            public String getName() {
                return "PollCreation";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return POLL_FEE;
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingPollCreation(buffer);
            }

            @Override
            Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingPollCreation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                Poll.addPoll(transaction, attachment);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {

                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();

                int optionsCount = attachment.getPollOptions().length;

                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollName().isEmpty()
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || optionsCount > Constants.MAX_POLL_OPTION_COUNT
                        || optionsCount == 0) {
                    throw new AplException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
                }

                if (attachment.getMinNumberOfOptions() < 1
                        || attachment.getMinNumberOfOptions() > optionsCount) {
                    throw new AplException.NotValidException("Invalid min number of options: " + attachment.getJSONObject());
                }

                if (attachment.getMaxNumberOfOptions() < 1
                        || attachment.getMaxNumberOfOptions() < attachment.getMinNumberOfOptions()
                        || attachment.getMaxNumberOfOptions() > optionsCount) {
                    throw new AplException.NotValidException("Invalid max number of options: " + attachment.getJSONObject());
                }

                for (int i = 0; i < optionsCount; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH
                            || attachment.getPollOptions()[i].isEmpty()) {
                        throw new AplException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }

                if (attachment.getMinRangeValue() < Constants.MIN_VOTE_VALUE || attachment.getMaxRangeValue() > Constants.MAX_VOTE_VALUE
                        || attachment.getMaxRangeValue() < attachment.getMinRangeValue()) {
                    throw new AplException.NotValidException("Invalid range: " + attachment.getJSONObject());
                }

                if (attachment.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1
                        || attachment.getFinishHeight() >= attachment.getFinishValidationHeight(transaction) + Constants.MAX_POLL_DURATION) {
                    throw new AplException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
                }

                if (! attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                    throw new AplException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
                }

                attachment.getVoteWeighting().validate();

            }

            @Override
            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(Messaging.POLL_CREATION, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "VoteCasting";
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingVoteCasting(buffer);
            }

            @Override
            Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingVoteCasting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                Vote.addVote(transaction, attachment);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {

                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == 0 || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new AplException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }

                long pollId = attachment.getPollId();

                Poll poll = Poll.getPoll(pollId);
                if (poll == null) {
                    throw new AplException.NotCurrentlyValidException("Invalid poll: " + Long.toUnsignedString(attachment.getPollId()));
                }

                if (Vote.getVote(pollId, transaction.getSenderId()) != null) {
                    throw new AplException.NotCurrentlyValidException("Double voting attempt");
                }

                if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction)) {
                    throw new AplException.NotCurrentlyValidException("Voting for this poll finishes at " + poll.getFinishHeight());
                }

                byte[] votes = attachment.getPollVote();
                int positiveCount = 0;
                for (byte vote : votes) {
                    if (vote != Constants.NO_VOTE_VALUE && (vote < poll.getMinRangeValue() || vote > poll.getMaxRangeValue())) {
                        throw new AplException.NotValidException(String.format("Invalid vote %d, vote must be between %d and %d",
                                vote, poll.getMinRangeValue(), poll.getMaxRangeValue()));
                    }
                    if (vote != Constants.NO_VOTE_VALUE) {
                        positiveCount++;
                    }
                }

                if (positiveCount < poll.getMinNumberOfOptions() || positiveCount > poll.getMaxNumberOfOptions()) {
                    throw new AplException.NotValidException(String.format("Invalid num of choices %d, number of choices must be between %d and %d",
                            positiveCount, poll.getMinNumberOfOptions(), poll.getMaxNumberOfOptions()));
                }
            }

            @Override
            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
                return isDuplicate(Messaging.VOTE_CASTING, key, duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PHASING_VOTE_CASTING = new Messaging() {

            private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                return attachment.getTransactionFullHashes().size() * Constants.ONE_APL;
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.PHASING_VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "PhasingVoteCasting";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return PHASING_VOTE_FEE;
            }

            @Override
            Attachment.MessagingPhasingVoteCasting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(buffer);
            }

            @Override
            Attachment.MessagingPhasingVoteCasting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(attachmentData);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {

                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                byte[] revealedSecret = attachment.getRevealedSecret();
                if (revealedSecret.length > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                    throw new AplException.NotValidException("Invalid revealed secret length " + revealedSecret.length);
                }
                byte[] hashedSecret = null;
                byte algorithm = 0;

                List<byte[]> hashes = attachment.getTransactionFullHashes();
                if (hashes.size() > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
                    throw new AplException.NotValidException("No more than " + Constants.MAX_PHASING_VOTE_TRANSACTIONS + " votes allowed for two-phased multi-voting");
                }

                long voterId = transaction.getSenderId();
                for (byte[] hash : hashes) {
                    long phasedTransactionId = Convert.fullHashToId(hash);
                    if (phasedTransactionId == 0) {
                        throw new AplException.NotValidException("Invalid phased transactionFullHash " + Convert.toHexString(hash));
                    }

                    PhasingPoll poll = PhasingPoll.getPoll(phasedTransactionId);
                    if (poll == null) {
                        throw new AplException.NotCurrentlyValidException("Invalid phased transaction " + Long.toUnsignedString(phasedTransactionId)
                                + ", or phasing is finished");
                    }
                    if (! poll.getVoteWeighting().acceptsVotes()) {
                        throw new AplException.NotValidException("This phased transaction does not require or accept voting");
                    }
                    long[] whitelist = poll.getWhitelist();
                    if (whitelist.length > 0 && Arrays.binarySearch(whitelist, voterId) < 0) {
                        throw new AplException.NotValidException("Voter is not in the phased transaction whitelist");
                    }
                    if (revealedSecret.length > 0) {
                        if (poll.getVoteWeighting().getVotingModel() != VoteWeighting.VotingModel.HASH) {
                            throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " does not accept by-hash voting");
                        }
                        if (hashedSecret != null && !Arrays.equals(poll.getHashedSecret(), hashedSecret)) {
                            throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecret");
                        }
                        if (algorithm != 0 && algorithm != poll.getAlgorithm()) {
                            throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecretAlgorithm");
                        }
                        if (hashedSecret == null && ! poll.verifySecret(revealedSecret)) {
                            throw new AplException.NotValidException("Revealed secret does not match phased transaction hashed secret");
                        }
                        hashedSecret = poll.getHashedSecret();
                        algorithm = poll.getAlgorithm();
                    } else if (poll.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                        throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " requires revealed secret for approval");
                    }
                    if (!Arrays.equals(poll.getFullHash(), hash)) {
                        throw new AplException.NotCurrentlyValidException("Phased transaction hash does not match hash in voting transaction");
                    }
                    if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1) {
                        throw new AplException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d",
                                poll.getFinishHeight(), attachment.getFinishValidationHeight(transaction) + 1));
                    }
                }
            }

            @Override
            final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                List<byte[]> hashes = attachment.getTransactionFullHashes();
                for (byte[] hash : hashes) {
                    PhasingVote.addVote(transaction, senderAccount, Convert.fullHashToId(hash));
                }
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            private final Fee ACCOUNT_INFO_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                    return attachment.getName().length() + attachment.getDescription().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_INFO;
            }

            @Override
            public String getName() {
                return "AccountInfo";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_INFO_FEE;
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer);
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
                    throw new AplException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
            }

            @Override
            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(Messaging.ACCOUNT_INFO, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_PROPERTY = new Messaging() {

            private final Fee ACCOUNT_PROPERTY_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                    return attachment.getValue().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_PROPERTY;
            }

            @Override
            public String getName() {
                return "AccountProperty";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_PROPERTY_FEE;
            }

            @Override
            Attachment.MessagingAccountProperty parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAccountProperty(buffer);
            }

            @Override
            Attachment.MessagingAccountProperty parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAccountProperty(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty)transaction.getAttachment();
                if (attachment.getProperty().length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH
                        || attachment.getProperty().length() == 0
                        || attachment.getValue().length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
                    throw new AplException.NotValidException("Invalid account property: " + attachment.getJSONObject());
                }
                if (transaction.getAmountATM() != 0) {
                    throw new AplException.NotValidException("Account property transaction cannot be used to send " + Constants.COIN_SYMBOL);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Setting Genesis account properties not allowed");
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                recipientAccount.setProperty(transaction, senderAccount, attachment.getProperty(), attachment.getValue());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_PROPERTY_DELETE = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public String getName() {
                return "AccountPropertyDelete";
            }

            @Override
            Attachment.MessagingAccountPropertyDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(buffer);
            }

            @Override
            Attachment.MessagingAccountPropertyDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete)transaction.getAttachment();
                Account.AccountProperty accountProperty = Account.getProperty(attachment.getPropertyId());
                if (accountProperty == null) {
                    throw new AplException.NotCurrentlyValidException("No such property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getSenderId() && accountProperty.getSetterId() != transaction.getSenderId()) {
                    throw new AplException.NotValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                            + " cannot delete property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getRecipientId()) {
                    throw new AplException.NotValidException("Account property " + Long.toUnsignedString(attachment.getPropertyId())
                            + " does not belong to " + Long.toUnsignedString(transaction.getRecipientId()));
                }
                if (transaction.getAmountATM() != 0) {
                    throw new AplException.NotValidException("Account property transaction cannot be used to send " + Constants.COIN_SYMBOL);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Deleting Genesis account properties not allowed");
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete) transaction.getAttachment();
                senderAccount.deleteProperty(attachment.getPropertyId());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

    }

    public static abstract class ColoredCoins extends TransactionType {

        private ColoredCoins() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_COLORED_COINS;
        }

        public static final TransactionType ASSET_ISSUANCE = new ColoredCoins() {

            private final Fee SINGLETON_ASSET_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL, 32) {
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                    return attachment.getDescription().length();
                }
            };

            private final Fee ASSET_ISSUANCE_FEE = (transaction, appendage) -> isSingletonIssuance(transaction) ?
                    SINGLETON_ASSET_FEE.getFee(transaction, appendage) : 1000 * Constants.ONE_APL;

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_ISSUANCE;
            }

            @Override
            public String getName() {
                return "AssetIssuance";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ASSET_ISSUANCE_FEE;
            }

            @Override
            long[] getBackFees(Transaction transaction) {
                if (isSingletonIssuance(transaction)) {
                    return Convert.EMPTY_LONG;
                }
                long feeATM = transaction.getFeeATM();
                return new long[] {feeATM * 3 / 10, feeATM * 2 / 10, feeATM / 10};
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(buffer);
            }

            @Override
            Attachment.ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                long assetId = transaction.getId();
                Asset.addAsset(transaction, attachment);
                senderAccount.addToAssetAndUnconfirmedAssetBalanceATU(getLedgerEvent(), assetId, assetId, attachment.getQuantityATU());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                        || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                        || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                        || attachment.getQuantityATU() <= 0
                        || attachment.getQuantityATU() > Constants.MAX_ASSET_QUANTITY_ATU
                        ) {
                    throw new AplException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
                }
                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new AplException.NotValidException("Invalid asset name: " + normalizedName);
                    }
                }
            }

            @Override
            boolean isBlockDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                return !isSingletonIssuance(transaction) && isDuplicate(ColoredCoins.ASSET_ISSUANCE, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

            private boolean isSingletonIssuance(Transaction transaction) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance)transaction.getAttachment();
                return attachment.getQuantityATU() == 1 && attachment.getDecimals() == 0
                        && attachment.getDescription().length() <= Constants.MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH;
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_TRANSFER;
            }

            @Override
            public String getName() {
                return "AssetTransfer";
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(buffer);
            }

            @Override
            Attachment.ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
                    senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityATU());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(),
                        -attachment.getQuantityATU());
                if (recipientAccount.getId() == Genesis.CREATOR_ID) {
                    Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
                } else {
                    recipientAccount.addToAssetAndUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), attachment.getQuantityATU());
                    AssetTransfer.addAssetTransfer(transaction, attachment);
                }
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityATU());
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer)transaction.getAttachment();
                if (transaction.getAmountATM() != 0 || attachment.getAssetId() == 0) {
                    throw new AplException.NotValidException("Invalid asset transfer amount or asset: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Asset transfer to Genesis not allowed, "
                            + "use asset delete attachment instead");
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
                    throw new AplException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType ASSET_DELETE = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_DELETE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_DELETE;
            }

            @Override
            public String getName() {
                return "AssetDelete";
            }

            @Override
            Attachment.ColoredCoinsAssetDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetDelete(buffer);
            }

            @Override
            Attachment.ColoredCoinsAssetDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAssetDelete(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete)transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
                    senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityATU());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete)transaction.getAttachment();
                senderAccount.addToAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(),
                        -attachment.getQuantityATU());
                Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete)transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityATU());
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete)transaction.getAttachment();
                if (attachment.getAssetId() == 0) {
                    throw new AplException.NotValidException("Invalid asset identifier: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
                    throw new AplException.NotValidException("Invalid asset delete asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            @Override
            final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement)transaction.getAttachment();
                if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > Constants.MAX_BALANCE_ATM
                        || attachment.getAssetId() == 0) {
                    throw new AplException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
                    throw new AplException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public final boolean canHaveRecipient() {
                return false;
            }

            @Override
            public final boolean isPhasingSafe() {
                return true;
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_ASK_ORDER_PLACEMENT;
            }

            @Override
            public String getName() {
                return "AskOrderPlacement";
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(buffer);
            }

            @Override
            Attachment.ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
                    senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityATU());
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                Order.Ask.addOrder(transaction, attachment);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityATU());
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_BID_ORDER_PLACEMENT;
            }

            @Override
            public String getName() {
                return "BidOrderPlacement";
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(buffer);
            }

            @Override
            Attachment.ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM())) {
                    senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                            -Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                Order.Bid.addOrder(transaction, attachment);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                        Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            @Override
            final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.ColoredCoinsOrderCancellation attachment = (Attachment.ColoredCoinsOrderCancellation) transaction.getAttachment();
                return TransactionType.isDuplicate(ColoredCoins.ASK_ORDER_CANCELLATION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
            }

            @Override
            public final boolean canHaveRecipient() {
                return false;
            }

            @Override
            public final boolean isPhasingSafe() {
                return true;
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_ASK_ORDER_CANCELLATION;
            }

            @Override
            public String getName() {
                return "AskOrderCancellation";
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(buffer);
            }

            @Override
            Attachment.ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order order = Order.Ask.getAskOrder(attachment.getOrderId());
                Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(),
                            order.getAssetId(), order.getQuantityATU());
                }
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order ask = Order.Ask.getAskOrder(attachment.getOrderId());
                if (ask == null) {
                    throw new AplException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
                }
                if (ask.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                            + Long.toUnsignedString(ask.getAccountId()));
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_BID_ORDER_CANCELLATION;
            }

            @Override
            public String getName() {
                return "BidOrderCancellation";
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(buffer);
            }

            @Override
            Attachment.ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order order = Order.Bid.getBidOrder(attachment.getOrderId());
                Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                            Math.multiplyExact(order.getQuantityATU(), order.getPriceATM()));
                }
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
                if (bid == null) {
                    throw new AplException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
                }
                if (bid.getAccountId() != transaction.getSenderId()) {
                    throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                            + Long.toUnsignedString(bid.getAccountId()));
                }
            }

        };

        public static final TransactionType DIVIDEND_PAYMENT = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ASSET_DIVIDEND_PAYMENT;
            }

            @Override
            public String getName() {
                return "DividendPayment";
            }

            @Override
            Attachment.ColoredCoinsDividendPayment parseAttachment(ByteBuffer buffer) {
                return new Attachment.ColoredCoinsDividendPayment(buffer);
            }

            @Override
            Attachment.ColoredCoinsDividendPayment parseAttachment(JSONObject attachmentData) {
                return new Attachment.ColoredCoinsDividendPayment(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment)transaction.getAttachment();
                long assetId = attachment.getAssetId();
                Asset asset = Asset.getAsset(assetId, attachment.getHeight());
                if (asset == null) {
                    return true;
                }
                long quantityATU = asset.getQuantityATU() - senderAccount.getAssetBalanceATU(assetId, attachment.getHeight());
                long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
                if (senderAccount.getUnconfirmedBalanceATM() >= totalDividendPayment) {
                    senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -totalDividendPayment);
                    return true;
                }
                return false;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment)transaction.getAttachment();
                senderAccount.payDividends(transaction.getId(), attachment);
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment)transaction.getAttachment();
                long assetId = attachment.getAssetId();
                Asset asset = Asset.getAsset(assetId, attachment.getHeight());
                if (asset == null) {
                    return;
                }
                long quantityATU = asset.getQuantityATU() - senderAccount.getAssetBalanceATU(assetId, attachment.getHeight());
                long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), totalDividendPayment);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment)transaction.getAttachment();
                if (attachment.getHeight() > Apl.getBlockchain().getHeight()) {
                    throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight()
                            + ", must not exceed current blockchain height " + Apl.getBlockchain().getHeight());
                }
                if (attachment.getHeight() <= attachment.getFinishValidationHeight(transaction) - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK) {
                    throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight()
                            + ", must be less than " + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK
                            + " blocks before " + attachment.getFinishValidationHeight(transaction));
                }
                Asset asset = Asset.getAsset(attachment.getAssetId(), attachment.getHeight());
                if (asset == null) {
                    throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId())
                            + " for dividend payment doesn't exist yet");
                }
                if (asset.getAccountId() != transaction.getSenderId() || attachment.getAmountATMPerATU() <= 0) {
                    throw new AplException.NotValidException("Invalid dividend payment sender or amount " + attachment.getJSONObject());
                }
                AssetDividend lastDividend = AssetDividend.getLastDividend(attachment.getAssetId());
                if (lastDividend != null && lastDividend.getHeight() > Apl.getBlockchain().getHeight() - 60) {
                    throw new AplException.NotCurrentlyValidException("Last dividend payment for asset " + Long.toUnsignedString(attachment.getAssetId())
                            + " was less than 60 blocks ago at " + lastDividend.getHeight() + ", current height is " + Apl.getBlockchain().getHeight()
                            + ", limit is one dividend per 60 blocks");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                return isDuplicate(ColoredCoins.DIVIDEND_PAYMENT, Long.toUnsignedString(attachment.getAssetId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class DigitalGoods extends TransactionType {

        private DigitalGoods() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DIGITAL_GOODS;
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Invalid digital goods transaction");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws AplException.ValidationException;


        public static final TransactionType LISTING = new DigitalGoods() {

            private final Fee DGS_LISTING_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                    return attachment.getName().length() + attachment.getDescription().length();
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_LISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsListing";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return DGS_LISTING_FEE;
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsListing(buffer);
            }

            @Override
            Attachment.DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsListing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                DigitalGoodsStore.listGoods(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                if (attachment.getName().length() == 0
                        || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
                        || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
                        || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceATM() <= 0 || attachment.getPriceATM() > Constants.MAX_BALANCE_ATM) {
                    throw new AplException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
                }
                Appendix.PrunablePlainMessage prunablePlainMessage = transaction.getPrunablePlainMessage();
                if (prunablePlainMessage != null) {
                    byte[] image = prunablePlainMessage.getMessage();
                    if (image != null) {
                        Tika tika = new Tika();
                        MediaType mediaType = null;
                        try {
                            String mediaTypeName = tika.detect(image);
                            mediaType = MediaType.parse(mediaTypeName);
                        } catch (NoClassDefFoundError e) {
                            Logger.logErrorMessage("Error running Tika parsers", e);
                        }
                        if (mediaType == null || !"image".equals(mediaType.getType())) {
                            throw new AplException.NotValidException("Only image attachments allowed for DGS listing, media type is " + mediaType);
                        }
                    }
                }
            }

            @Override
            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(DigitalGoods.LISTING, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_DELISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelisting";
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(buffer);
            }

            @Override
            Attachment.DigitalGoodsDelisting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.delistGoods(attachment.getGoodsId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
                    throw new AplException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPriceChange";
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(buffer);
            }

            @Override
            Attachment.DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceATM());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > Constants.MAX_BALANCE_ATM
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new AplException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsQuantityChange";
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(buffer);
            }

            @Override
            Attachment.DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new AplException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_PURCHASE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPurchase";
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(buffer);
            }

            @Override
            Attachment.DigitalGoodsPurchase parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM())) {
                    senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                            -Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                        Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceATM() <= 0 || attachment.getPriceATM() > Constants.MAX_BALANCE_ATM
                        || (goods != null && goods.getSellerId() != transaction.getRecipientId())) {
                    throw new AplException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new AplException.NotValidException("Only text encrypted messages allowed");
                }
                if (goods == null || goods.isDelisted()) {
                    throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
                if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceATM() != goods.getPriceATM()) {
                    throw new AplException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
                }
                if (attachment.getDeliveryDeadlineTimestamp() <= Apl.getBlockchain().getLastBlockTimestamp()) {
                    throw new AplException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, false);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            private final Fee DGS_DELIVERY_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                    return attachment.getGoodsDataLength() - 16;
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_DELIVERY;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelivery";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return DGS_DELIVERY_FEE;
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(buffer);
            }

            @Override
            Attachment.DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                if (attachmentData.get("goodsData") == null) {
                    return new Attachment.UnencryptedDigitalGoodsDelivery(attachmentData);
                }
                return new Attachment.DigitalGoodsDelivery(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPendingPurchase(attachment.getPurchaseId());
                if (attachment.getGoodsDataLength() > Constants.MAX_DGS_GOODS_LENGTH) {
                    throw new AplException.NotValidException("Invalid digital goods delivery data length: " + attachment.getGoodsDataLength());
                }
                if (attachment.getGoods() != null) {
                    if (attachment.getGoods().getData().length == 0 || attachment.getGoods().getNonce().length != 32) {
                        throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getDiscountATM() < 0 || attachment.getDiscountATM() > Constants.MAX_BALANCE_ATM
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()
                                || attachment.getDiscountATM() > Math.multiplyExact(purchase.getPriceATM(), (long) purchase.getQuantity())))) {
                    throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
                if (purchase == null || purchase.getEncryptedGoods() != null) {
                    throw new AplException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: "
                            + attachment.getJSONObject());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELIVERY, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            public String getName() {
                return "DigitalGoodsFeedback";
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(buffer);
            }

            @Override
            Attachment.DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (purchase != null &&
                        (purchase.getSellerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getBuyerId())) {
                    throw new AplException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                    throw new AplException.NotValidException("Missing feedback message");
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new AplException.NotValidException("Only text encrypted messages allowed");
                }
                if (transaction.getMessage() != null && ! transaction.getMessage().isText()) {
                    throw new AplException.NotValidException("Only text public messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null) {
                    throw new AplException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.DIGITAL_GOODS_REFUND;
            }

            @Override
            public String getName() {
                return "DigitalGoodsRefund";
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsRefund(buffer);
            }

            @Override
            Attachment.DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.DigitalGoodsRefund(attachmentData);
            }

            @Override
            boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getRefundATM()) {
                    senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -attachment.getRefundATM());
                    return true;
                }
                return false;
            }

            @Override
            void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), attachment.getRefundATM());
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.refund(getLedgerEvent(), transaction.getId(), transaction.getSenderId(),
                        attachment.getPurchaseId(), attachment.getRefundATM(), transaction.getEncryptedMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundATM() < 0 || attachment.getRefundATM() > Constants.MAX_BALANCE_ATM
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()))) {
                    throw new AplException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new AplException.NotValidException("Only text encrypted messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundATM() != 0) {
                    throw new AplException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return isDuplicate(DigitalGoods.REFUND, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class AccountControl extends TransactionType {

        private AccountControl() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_ACCOUNT_CONTROL;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            public String getName() {
                return "EffectiveBalanceLeasing";
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(buffer);
            }

            @Override
            Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(attachmentData);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing)transaction.getAttachment();
                if (transaction.getSenderId() == transaction.getRecipientId()) {
                    throw new AplException.NotValidException("Account cannot lease balance to itself");
                }
                if (transaction.getAmountATM() != 0) {
                    throw new AplException.NotValidException("Transaction amount must be 0 for effective balance leasing");
                }
                if (attachment.getPeriod() < Constants.LEASING_DELAY || attachment.getPeriod() > 65535) {
                    throw new AplException.NotValidException("Invalid effective balance leasing period: " + attachment.getPeriod());
                }
                byte[] recipientPublicKey = Account.getPublicKey(transaction.getRecipientId());
                if (recipientPublicKey == null) {
                    throw new AplException.NotCurrentlyValidException("Invalid effective balance leasing: "
                            + " recipient account " + Long.toUnsignedString(transaction.getRecipientId()) + " not found or no public key published");
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new AplException.NotValidException("Leasing to Genesis account not allowed");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType SET_PHASING_ONLY = new AccountControl() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_CONTROL_PHASING_ONLY;
            }

            @Override
            AbstractAttachment parseAttachment(ByteBuffer buffer) {
                return new Attachment.SetPhasingOnly(buffer);
            }

            @Override
            AbstractAttachment parseAttachment(JSONObject attachmentData) {
                return new Attachment.SetPhasingOnly(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws ValidationException {
                Attachment.SetPhasingOnly attachment = (Attachment.SetPhasingOnly)transaction.getAttachment();
                VotingModel votingModel = attachment.getPhasingParams().getVoteWeighting().getVotingModel();
                attachment.getPhasingParams().validate();
                if (votingModel == VotingModel.NONE) {
                    Account senderAccount = Account.getAccount(transaction.getSenderId());
                    if (senderAccount == null || !senderAccount.getControls().contains(ControlType.PHASING_ONLY)) {
                        throw new AplException.NotCurrentlyValidException("Phasing only account control is not currently enabled");
                    }
                } else if (votingModel == VotingModel.TRANSACTION || votingModel == VotingModel.HASH) {
                    throw new AplException.NotValidException("Invalid voting model " + votingModel + " for account control");
                }
                long maxFees = attachment.getMaxFees();
                long maxFeesLimit = (attachment.getPhasingParams().getVoteWeighting().isBalanceIndependent() ? 3 : 22) * Constants.ONE_APL;
                if (maxFees < 0 || (maxFees > 0 && maxFees < maxFeesLimit) || maxFees > Constants.MAX_BALANCE_ATM) {
                    throw new AplException.NotValidException(String.format("Invalid max fees %f %s", ((double)maxFees)/Constants.ONE_APL, Constants.COIN_SYMBOL));
                }
                short minDuration = attachment.getMinDuration();
                if (minDuration < 0 || (minDuration > 0 && minDuration < 3) || minDuration >= Constants.MAX_PHASING_DURATION) {
                    throw new AplException.NotValidException("Invalid min duration " + attachment.getMinDuration());
                }
                short maxDuration = attachment.getMaxDuration();
                if (maxDuration < 0 || (maxDuration > 0 && maxDuration < 3) || maxDuration >= Constants.MAX_PHASING_DURATION) {
                    throw new AplException.NotValidException("Invalid max duration " + maxDuration);
                }
                if (minDuration > maxDuration) {
                    throw new AplException.NotValidException(String.format("Min duration %d cannot exceed max duration %d ",
                            minDuration, maxDuration));
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return TransactionType.isDuplicate(SET_PHASING_ONLY, Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.SetPhasingOnly attachment = (Attachment.SetPhasingOnly)transaction.getAttachment();
                AccountRestrictions.PhasingOnly.set(senderAccount, attachment);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public String getName() {
                return "SetPhasingOnly";
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class Data extends TransactionType {

        private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL/10) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return appendix.getFullSize();
            }
        };

        private Data() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DATA;
        }

        @Override
        final Fee getBaselineFee(Transaction transaction) {
            return TAGGED_DATA_FEE;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        @Override
        public final boolean isPhasable() {
            return false;
        }

        public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.TAGGED_DATA_UPLOAD;
            }

            @Override
            Attachment.TaggedDataUpload parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.TaggedDataUpload(buffer);
            }

            @Override
            Attachment.TaggedDataUpload parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.TaggedDataUpload(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                if (attachment.getData() == null && Apl.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                if (attachment.getData() != null) {
                    if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                        throw new AplException.NotValidException("Invalid name length: " + attachment.getName().length());
                    }
                    if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                        throw new AplException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                    }
                    if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                        throw new AplException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                    }
                    if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                        throw new AplException.NotValidException("Invalid type length: " + attachment.getType().length());
                    }
                    if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                        throw new AplException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                    }
                    if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                        throw new AplException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                    }
                    if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                        throw new AplException.NotValidException("Invalid data length: " + attachment.getData().length);
                    }
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                TaggedData.add((TransactionImpl)transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataUpload";
            }

            @Override
            boolean isPruned(long transactionId) {
                return TaggedData.isPruned(transactionId);
            }

        };

        public static final TransactionType TAGGED_DATA_EXTEND = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.TAGGED_DATA_EXTEND;
            }

            @Override
            Attachment.TaggedDataExtend parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.TaggedDataExtend(buffer);
            }

            @Override
            Attachment.TaggedDataExtend parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.TaggedDataExtend(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws AplException.ValidationException {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                if ((attachment.jsonIsPruned() || attachment.getData() == null) && Apl.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                TransactionImpl uploadTransaction = TransactionDb.findTransaction(attachment.getTaggedDataId(), Apl.getBlockchain().getHeight());
                if (uploadTransaction == null) {
                    throw new AplException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
                }
                if (uploadTransaction.getType() != TAGGED_DATA_UPLOAD) {
                    throw new AplException.NotValidException("Transaction " + Long.toUnsignedString(attachment.getTaggedDataId())
                            + " is not a tagged data upload");
                }
                if (attachment.getData() != null) {
                    Attachment.TaggedDataUpload taggedDataUpload = (Attachment.TaggedDataUpload)uploadTransaction.getAttachment();
                    if (!Arrays.equals(attachment.getHash(), taggedDataUpload.getHash())) {
                        throw new AplException.NotValidException("Hashes don't match! Extend hash: " + Convert.toHexString(attachment.getHash())
                                + " upload hash: " + Convert.toHexString(taggedDataUpload.getHash()));
                    }
                }
                TaggedData taggedData = TaggedData.getData(attachment.getTaggedDataId());
                if (taggedData != null && taggedData.getTransactionTimestamp() > Apl.getEpochTime() + 6 * Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new AplException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                TaggedData.extend(transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataExtend";
            }

            @Override
            boolean isPruned(long transactionId) {
                return false;
            }

        };

    }

    public static abstract class Update extends TransactionType {

        private final Fee UPDATE_FEE = new Fee.ConstantFee(Constants.ONE_APL);

        private Update() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_UPDATE;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
            if (attachment.getUrl().getFirst().length != Constants.UPDATE_URL_PART_LENGTH
                    || attachment.getUrl().getSecond().length != Constants.UPDATE_URL_PART_LENGTH
                    || attachment.getHash().length > Constants.MAX_UPDATE_HASH_LENGTH) {
                throw new AplException.NotValidException("Invalid update transaction attachment:" +  attachment.getJSONObject());
            }
        }

        public static boolean isUpdate(TransactionType transactionType) {
            return transactionType.getType() == TYPE_UPDATE;
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {}

        @Override
        Fee getBaselineFee(Transaction transaction) {
            return UPDATE_FEE;
        }

        public static final TransactionType CRITICAL = new Update() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_UPDATE_CRITICAL;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.UPDATE_CRITICAL;
            }

            @Override
            public String getName() {
                return "CriticalUpdate";
            }

            @Override
            Attachment.CriticalUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.CriticalUpdate(buffer);
            }

            @Override
            Attachment.CriticalUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.CriticalUpdate(attachmentData);
            }

        };

        public static final TransactionType IMPORTANT = new Update() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_UPDATE_IMPORTANT;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.UPDATE_IMPORTANT;
            }

            @Override
            public String getName() {
                return "ImportantUpdate";
            }

            @Override
            Attachment.ImportantUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.ImportantUpdate(buffer);
            }

            @Override
            Attachment.ImportantUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.ImportantUpdate(attachmentData);
            }
        };

        public static final TransactionType MINOR = new Update() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_UPDATE_MINOR;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.UPDATE_MINOR;
            }

            @Override
            public String getName() {
                return "MinorUpdate";
            }

            @Override
            Attachment.MinorUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
                return new Attachment.MinorUpdate(buffer);
            }

            @Override
            Attachment.MinorUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
                return new Attachment.MinorUpdate(attachmentData);
            }
        };
    }
}
