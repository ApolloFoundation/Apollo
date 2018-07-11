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

import com.apollocurrency.aplwallet.apl.AccountLedger.LedgerEvent;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public abstract class MonetarySystem extends TransactionType {

    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE = 0;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE = 1;
    private static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM = 2;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER = 3;
    private static final byte SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER = 4;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY = 5;
    private static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL = 6;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING = 7;
    private static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION = 8;

    static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE:
                return MonetarySystem.CURRENCY_ISSUANCE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE:
                return MonetarySystem.RESERVE_INCREASE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM:
                return MonetarySystem.RESERVE_CLAIM;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER:
                return MonetarySystem.CURRENCY_TRANSFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER:
                return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY:
                return MonetarySystem.EXCHANGE_BUY;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL:
                return MonetarySystem.EXCHANGE_SELL;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING:
                return MonetarySystem.CURRENCY_MINTING;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION:
                return MonetarySystem.CURRENCY_DELETION;
            default:
                return null;
        }
    }

    private MonetarySystem() {}

    @Override
    public final byte getType() {
        return TransactionType.TYPE_MONETARY_SYSTEM;
    }

    @Override
    boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        Attachment.MonetarySystemAttachment attachment = (Attachment.MonetarySystemAttachment) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, false);
        if (! nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, false);
        }
        return isDuplicate;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    public static final TransactionType CURRENCY_ISSUANCE = new MonetarySystem() {

        private final Fee FIVE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(40 * Constants.ONE_APL);
        private final Fee FOUR_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(1000 * Constants.ONE_APL);
        private final Fee THREE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(25000 * Constants.ONE_APL);

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_ISSUANCE;
        }

        @Override
        public String getName() {
            return "CurrencyIssuance";
        }

        @Override
        Fee getBaselineFee(Transaction transaction) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            int minLength = Math.min(attachment.getCode().length(), attachment.getName().length());
            Currency oldCurrency;
            int oldMinLength = Integer.MAX_VALUE;
            if ((oldCurrency = Currency.getCurrencyByCode(attachment.getCode())) != null) {
                oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
            }
            if ((oldCurrency = Currency.getCurrencyByCode(attachment.getName())) != null) {
                oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
            }
            if ((oldCurrency = Currency.getCurrencyByName(attachment.getName())) != null) {
                oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
            }
            if ((oldCurrency = Currency.getCurrencyByName(attachment.getCode())) != null) {
                oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
            }
            if (minLength >= oldMinLength) {
                return FIVE_LETTER_CURRENCY_ISSUANCE_FEE;
            }
            switch (minLength) {
                case 3:
                    return THREE_LETTER_CURRENCY_ISSUANCE_FEE;
                case 4:
                    return FOUR_LETTER_CURRENCY_ISSUANCE_FEE;
                case 5:
                    return FIVE_LETTER_CURRENCY_ISSUANCE_FEE;
                default:
                    // never, invalid code length will be checked and caught later
                    return THREE_LETTER_CURRENCY_ISSUANCE_FEE;
            }
        }

        @Override
        long[] getBackFees(Transaction transaction) {
            long feeATM = transaction.getFeeATM();
            return new long[] {feeATM * 3 / 10, feeATM * 2 / 10, feeATM / 10};
        }

        @Override
        Attachment.MonetarySystemCurrencyIssuance parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyIssuance(buffer);
        }

        @Override
        Attachment.MonetarySystemCurrencyIssuance parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyIssuance(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            String nameLower = attachment.getName().toLowerCase();
            String codeLower = attachment.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
            }
            return isDuplicate;
        }

        @Override
        boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return isDuplicate(CURRENCY_ISSUANCE, getName(), duplicates, true);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            if (attachment.getMaxSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY
                    || attachment.getMaxSupply() <= 0
                    || attachment.getInitialSupply() < 0
                    || attachment.getInitialSupply() > attachment.getMaxSupply()
                    || attachment.getReserveSupply() < 0
                    || attachment.getReserveSupply() > attachment.getMaxSupply()
                    || attachment.getIssuanceHeight() < 0
                    || attachment.getMinReservePerUnitATM() < 0
                    || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                    || attachment.getRuleset() != 0) {
                throw new AplException.NotValidException("Invalid currency issuance: " + attachment.getJSONObject());
            }
            int t = 1;
            for (int i = 0; i < 32; i++) {
                if ((t & attachment.getType()) != 0 && CurrencyType.get(t) == null) {
                    throw new AplException.NotValidException("Invalid currency type: " + attachment.getType());
                }
                t <<= 1;
            }
            CurrencyType.validate(attachment.getType(), transaction);
            CurrencyType.validateCurrencyNaming(transaction.getSenderId(), attachment);
        }


        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
            long transactionId = transaction.getId();
            Currency.addCurrency(getLedgerEvent(), transactionId, transaction, senderAccount, attachment);
            senderAccount.addToCurrencyAndUnconfirmedCurrencyUnits(getLedgerEvent(), transactionId,
                    transactionId, attachment.getInitialSupply());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_INCREASE = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_RESERVE_INCREASE;
        }

        @Override
        public String getName() {
            return "ReserveIncrease";
        }

        @Override
        Attachment.MonetarySystemReserveIncrease parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemReserveIncrease(buffer);
        }

        @Override
        Attachment.MonetarySystemReserveIncrease parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemReserveIncrease(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            if (attachment.getAmountPerUnitATM() <= 0) {
                throw new AplException.NotValidException("Reserve increase amount must be positive: " + attachment.getAmountPerUnitATM());
            }
            CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM())) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                        -Math.multiplyExact(currency.getReserveSupply(), attachment.getAmountPerUnitATM()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            long reserveSupply;
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (currency != null) {
                reserveSupply = currency.getReserveSupply();
            } else { // currency must have been deleted, get reserve supply from the original issuance transaction
                Transaction currencyIssuance = Apl.getBlockchain().getTransaction(attachment.getCurrencyId());
                Attachment.MonetarySystemCurrencyIssuance currencyIssuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) currencyIssuance.getAttachment();
                reserveSupply = currencyIssuanceAttachment.getReserveSupply();
            }
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                    Math.multiplyExact(reserveSupply, attachment.getAmountPerUnitATM()));
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
            Currency.increaseReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(),
                    attachment.getAmountPerUnitATM());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType RESERVE_CLAIM = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_RESERVE_CLAIM;
        }

        @Override
        public String getName() {
            return "ReserveClaim";
        }

        @Override
        Attachment.MonetarySystemReserveClaim parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemReserveClaim(buffer);
        }

        @Override
        Attachment.MonetarySystemReserveClaim parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemReserveClaim(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            if (attachment.getUnits() <= 0) {
                throw new AplException.NotValidException("Reserve claim number of units must be positive: " + attachment.getUnits());
            }
            CurrencyType.validate(Currency.getCurrency(attachment.getCurrencyId()), transaction);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getUnits()) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), -attachment.getUnits());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (currency != null) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(),
                        attachment.getUnits());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemReserveClaim attachment = (Attachment.MonetarySystemReserveClaim) transaction.getAttachment();
            Currency.claimReserve(getLedgerEvent(), transaction.getId(), senderAccount, attachment.getCurrencyId(),
                    attachment.getUnits());
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType CURRENCY_TRANSFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_TRANSFER;
        }

        @Override
        public String getName() {
            return "CurrencyTransfer";
        }

        @Override
        Attachment.MonetarySystemCurrencyTransfer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyTransfer(buffer);
        }

        @Override
        Attachment.MonetarySystemCurrencyTransfer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyTransfer(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            if (attachment.getUnits() <= 0) {
                throw new AplException.NotValidException("Invalid currency transfer: " + attachment.getJSONObject());
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new AplException.NotValidException("Currency transfer to genesis account not allowed");
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            if (attachment.getUnits() > senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId())) {
                return false;
            }
            senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                    attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (currency != null) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), attachment.getUnits());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyTransfer attachment = (Attachment.MonetarySystemCurrencyTransfer) transaction.getAttachment();
            Currency.transferCurrency(getLedgerEvent(), transaction.getId(), senderAccount, recipientAccount,
                    attachment.getCurrencyId(), attachment.getUnits());
            CurrencyTransfer.addTransfer(transaction, attachment);
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

    };

    public static final TransactionType PUBLISH_EXCHANGE_OFFER = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public String getName() {
            return "PublishExchangeOffer";
        }

        @Override
        Attachment.MonetarySystemPublishExchangeOffer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemPublishExchangeOffer(buffer);
        }

        @Override
        Attachment.MonetarySystemPublishExchangeOffer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemPublishExchangeOffer(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            if (attachment.getBuyRateATM() <= 0
                    || attachment.getSellRateATM() <= 0
                    || attachment.getBuyRateATM() > attachment.getSellRateATM()) {
                throw new AplException.NotValidException(String.format("Invalid exchange offer, buy rate %d and sell rate %d has to be larger than 0, buy rate cannot be larger than sell rate",
                        attachment.getBuyRateATM(), attachment.getSellRateATM()));
            }
            if (attachment.getTotalBuyLimit() < 0
                    || attachment.getTotalSellLimit() < 0
                    || attachment.getInitialBuySupply() < 0
                    || attachment.getInitialSellSupply() < 0
                    || attachment.getExpirationHeight() < 0) {
                throw new AplException.NotValidException("Invalid exchange offer, units and height cannot be negative: " + attachment.getJSONObject());
            }
            if (attachment.getTotalBuyLimit() < attachment.getInitialBuySupply()
                    || attachment.getTotalSellLimit() < attachment.getInitialSellSupply()) {
                throw new AplException.NotValidException("Initial supplies must not exceed total limits");
            }
            if (attachment.getTotalBuyLimit() == 0 && attachment.getTotalSellLimit() == 0) {
                throw new AplException.NotValidException("Total buy and sell limits cannot be both 0");
            }
            if (attachment.getInitialBuySupply() == 0 && attachment.getInitialSellSupply() == 0) {
                throw new AplException.NotValidException("Initial buy and sell supply cannot be both 0");
            }
            if (attachment.getExpirationHeight() <= attachment.getFinishValidationHeight(transaction)) {
                throw new AplException.NotCurrentlyValidException("Expiration height must be after transaction execution height");
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new AplException.NotCurrentlyValidException("Currency not currently active: " + attachment.getJSONObject());
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM())
                    && senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getInitialSellSupply()) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                        -Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), -attachment.getInitialSellSupply());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                    Math.multiplyExact(attachment.getInitialBuySupply(), attachment.getBuyRateATM()));
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (currency != null) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), attachment.getInitialSellSupply());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemPublishExchangeOffer attachment = (Attachment.MonetarySystemPublishExchangeOffer) transaction.getAttachment();
            CurrencyExchangeOffer.publishOffer(transaction, attachment);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    abstract static class MonetarySystemExchange extends MonetarySystem {

        @Override
        final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemExchange attachment = (Attachment.MonetarySystemExchange) transaction.getAttachment();
            if (attachment.getRateATM() <= 0 || attachment.getUnits() == 0) {
                throw new AplException.NotValidException("Invalid exchange: " + attachment.getJSONObject());
            }
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (! currency.isActive()) {
                throw new AplException.NotCurrentlyValidException("Currency not active: " + attachment.getJSONObject());
            }
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

    }

    public static final TransactionType EXCHANGE_BUY = new MonetarySystemExchange() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_EXCHANGE_BUY;
        }

        @Override
        public String getName() {
            return "ExchangeBuy";
        }

        @Override
        Attachment.MonetarySystemExchangeBuy parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemExchangeBuy(buffer);
        }

        @Override
        Attachment.MonetarySystemExchangeBuy parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemExchangeBuy(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return super.isDuplicate(transaction, duplicates);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getUnits(), attachment.getRateATM())) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                        -Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(),
                    Math.multiplyExact(attachment.getUnits(), attachment.getRateATM()));
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchangeBuy attachment = (Attachment.MonetarySystemExchangeBuy) transaction.getAttachment();
            ExchangeRequest.addExchangeRequest(transaction, attachment);
            CurrencyExchangeOffer.exchangeAPLForCurrency(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
        }

    };

    public static final TransactionType EXCHANGE_SELL = new MonetarySystemExchange() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_EXCHANGE_SELL;
        }

        @Override
        public String getName() {
            return "ExchangeSell";
        }

        @Override
        Attachment.MonetarySystemExchangeSell parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemExchangeSell(buffer);
        }

        @Override
        Attachment.MonetarySystemExchangeSell parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemExchangeSell(attachmentData);
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            if (senderAccount.getUnconfirmedCurrencyUnits(attachment.getCurrencyId()) >= attachment.getUnits()) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), -attachment.getUnits());
                return true;
            }
            return false;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            if (currency != null) {
                senderAccount.addToUnconfirmedCurrencyUnits(getLedgerEvent(), transaction.getId(),
                        attachment.getCurrencyId(), attachment.getUnits());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemExchangeSell attachment = (Attachment.MonetarySystemExchangeSell) transaction.getAttachment();
            ExchangeRequest.addExchangeRequest(transaction, attachment);
            CurrencyExchangeOffer.exchangeCurrencyForAPL(transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
        }

    };

    public static final TransactionType CURRENCY_MINTING = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_MINTING;
        }

        @Override
        public String getName() {
            return "CurrencyMinting";
        }

        @Override
        Attachment.MonetarySystemCurrencyMinting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyMinting(buffer);
        }

        @Override
        Attachment.MonetarySystemCurrencyMinting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyMinting(attachmentData);
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (attachment.getUnits() <= 0) {
                throw new AplException.NotValidException("Invalid number of units: " + attachment.getUnits());
            }
            if (attachment.getUnits() > (currency.getMaxSupply() - currency.getReserveSupply()) / Constants.MAX_MINTING_RATIO) {
                throw new AplException.NotValidException(String.format("Cannot mint more than 1/%d of the total units supply in a single request", Constants.MAX_MINTING_RATIO));
            }
            if (!currency.isActive()) {
                throw new AplException.NotCurrentlyValidException("Currency not currently active " + attachment.getJSONObject());
            }
            long counter = CurrencyMint.getCounter(attachment.getCurrencyId(), transaction.getSenderId());
            if (attachment.getCounter() <= counter) {
                throw new AplException.NotCurrentlyValidException(String.format("Counter %d has to be bigger than %d", attachment.getCounter(), counter));
            }
            if (!CurrencyMinting.meetsTarget(transaction.getSenderId(), currency, attachment)) {
                throw new AplException.NotCurrentlyValidException(String.format("Hash doesn't meet target %s", attachment.getJSONObject()));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            CurrencyMint.mintCurrency(getLedgerEvent(), transaction.getId(), senderAccount, attachment);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            return TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true)
                    || super.isDuplicate(transaction, duplicates);
        }

        @Override
        boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.MonetarySystemCurrencyMinting attachment = (Attachment.MonetarySystemCurrencyMinting) transaction.getAttachment();
            return TransactionType.isDuplicate(CURRENCY_MINTING, attachment.getCurrencyId() + ":" + transaction.getSenderId(), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

    public static final TransactionType CURRENCY_DELETION = new MonetarySystem() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CURRENCY_DELETION;
        }

        @Override
        public String getName() {
            return "CurrencyDeletion";
        }

        @Override
        Attachment.MonetarySystemCurrencyDeletion parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyDeletion(buffer);
        }

        @Override
        Attachment.MonetarySystemCurrencyDeletion parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MonetarySystemCurrencyDeletion(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.MonetarySystemCurrencyDeletion attachment = (Attachment.MonetarySystemCurrencyDeletion) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            String nameLower = currency.getName().toLowerCase();
            String codeLower = currency.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
            }
            return isDuplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment.MonetarySystemCurrencyDeletion attachment = (Attachment.MonetarySystemCurrencyDeletion) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            CurrencyType.validate(currency, transaction);
            if (!currency.canBeDeletedBy(transaction.getSenderId())) {
                throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(currency.getId()) + " cannot be deleted by account " +
                        Long.toUnsignedString(transaction.getSenderId()));
            }
        }

        @Override
        boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.MonetarySystemCurrencyDeletion attachment = (Attachment.MonetarySystemCurrencyDeletion) transaction.getAttachment();
            Currency currency = Currency.getCurrency(attachment.getCurrencyId());
            currency.delete(getLedgerEvent(), transaction.getId(), senderAccount);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

    };

}
