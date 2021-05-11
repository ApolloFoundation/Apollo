/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class MSCurrencyIssuanceTransactionType extends MonetarySystemTransactionType {

    public static final BigDecimal[] DEFAULT_FEES = {BigDecimal.valueOf(40), BigDecimal.valueOf(1000), BigDecimal.valueOf(25000)};
    private final Fee FIVE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(Math.multiplyExact(40, getBlockchainConfig().getOneAPL()));
    private final Fee FOUR_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(Math.multiplyExact(1000, getBlockchainConfig().getOneAPL()));
    private final Fee THREE_LETTER_CURRENCY_ISSUANCE_FEE = new Fee.ConstantFee(Math.multiplyExact(25000, getBlockchainConfig().getOneAPL()));

    private final CurrencyService currencyService;
    private final AccountCurrencyService accountCurrencyService;

    @Inject
    public MSCurrencyIssuanceTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService) {
        super(blockchainConfig, accountService, currencyService);
        this.currencyService = currencyService;
        this.accountCurrencyService = accountCurrencyService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE;
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
        BigDecimal[] additionalFees = getBlockchainConfig().getCurrentConfig().getAdditionalFees(getSpec(), DEFAULT_FEES);
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        int minLength = Math.min(attachment.getCode().length(), attachment.getName().length());
        Currency oldCurrency;
        int oldMinLength = Integer.MAX_VALUE;
        if ((oldCurrency = currencyService.getCurrencyByCode(attachment.getCode())) != null) {
            oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
        }
        if ((oldCurrency = currencyService.getCurrencyByCode(attachment.getName())) != null) {
            oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
        }
        if ((oldCurrency = currencyService.getCurrencyByName(attachment.getName())) != null) {
            oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
        }
        if ((oldCurrency = currencyService.getCurrencyByName(attachment.getCode())) != null) {
            oldMinLength = Math.min(oldMinLength, Math.min(oldCurrency.getCode().length(), oldCurrency.getName().length()));
        }
        Fee.ConstantFee fiveLetterFee = new Fee.ConstantFee(additionalFees[0].multiply(BigDecimal.valueOf(getBlockchainConfig().getOneAPL())).longValueExact());
        if (minLength >= oldMinLength) {
            return fiveLetterFee;
        }
        Fee.ConstantFee threeLettersFee = new Fee.ConstantFee(additionalFees[2].multiply(BigDecimal.valueOf(getBlockchainConfig().getOneAPL())).longValueExact());
        switch (minLength) {
            case 3:
                return threeLettersFee;
            case 4:
                return new Fee.ConstantFee(additionalFees[1].multiply(BigDecimal.valueOf(getBlockchainConfig().getOneAPL())).longValueExact());
            case 5:
                return fiveLetterFee;
            default:
                // never, invalid code length will be checked and caught later
                return threeLettersFee;
        }
    }

    @Override
    @TransactionFee({FeeMarker.FEE, FeeMarker.BACK_FEE})
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
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        String nameLower = attachment.getName().toLowerCase();
        String codeLower = attachment.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, nameLower, duplicates, true);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, codeLower, duplicates, true);
        }
        return isDuplicate;
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, getName(), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        currencyService.validateCurrencyNamingStateDependent(transaction.getSenderId(), attachment);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MonetarySystemCurrencyIssuance attachment = (MonetarySystemCurrencyIssuance) transaction.getAttachment();
        if (attachment.getMaxSupply() > Math.multiplyExact(getBlockchainConfig().getInitialSupply(), getBlockchainConfig().getOneAPL())
            || attachment.getMaxSupply() <= 0 || attachment.getInitialSupply() < 0
            || attachment.getInitialSupply() > attachment.getMaxSupply()
            || attachment.getReserveSupply() < 0
            || attachment.getReserveSupply() > attachment.getMaxSupply()
            || attachment.getIssuanceHeight() < 0 || attachment.getMinReservePerUnitATM() < 0
            || attachment.getDecimals() < 0 || attachment.getDecimals() > getBlockchainConfig().getDecimals()
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
        currencyService.validate(attachment.getType(), transaction);
        currencyService.validateCurrencyNamingStateIndependent(attachment);

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
        currencyService.addCurrency(getLedgerEvent(), transactionId, transaction, senderAccount, attachment);
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transactionId, transactionId, attachment.getInitialSupply());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
