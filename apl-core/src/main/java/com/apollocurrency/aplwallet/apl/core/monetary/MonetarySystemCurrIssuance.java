/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class MonetarySystemCurrIssuance extends MonetarySystem {
    
    public MonetarySystemCurrIssuance() {
    }
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
    public Fee getBaselineFee(Transaction transaction) {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
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
    public long[] getBackFees(Transaction transaction) {
        long feeATM = transaction.getFeeATM();
        return new long[]{feeATM * 3 / 10, feeATM * 2 / 10, feeATM / 10};
    }

    @Override
    public MonetarySystemCurrencyIssuance parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MonetarySystemCurrencyIssuance(buffer);
    }

    @Override
    public MonetarySystemCurrencyIssuance parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MonetarySystemCurrencyIssuance(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        String nameLower = attachment.getName().toLowerCase();
        String codeLower = attachment.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, true);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, true);
        }
        return isDuplicate;
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return isDuplicate(CURRENCY_ISSUANCE, getName(), duplicates, true);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        if (attachment.getMaxSupply() > Constants.MAX_CURRENCY_TOTAL_SUPPLY || attachment.getMaxSupply() <= 0 || attachment.getInitialSupply() < 0 || attachment.getInitialSupply() > attachment.getMaxSupply() || attachment.getReserveSupply() < 0 || attachment.getReserveSupply() > attachment.getMaxSupply() || attachment.getIssuanceHeight() < 0 || attachment.getMinReservePerUnitATM() < 0 || attachment.getDecimals() < 0 || attachment.getDecimals() > 8 || attachment.getRuleset() != 0) {
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
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        long transactionId = transaction.getId();
        Currency.addCurrency(getLedgerEvent(), transactionId, transaction, senderAccount, attachment);
        senderAccount.addToCurrencyAndUnconfirmedCurrencyUnits(getLedgerEvent(), transactionId, transactionId, attachment.getInitialSupply());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }
    
}
