/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCDividendPaymentAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.MathUtils;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class CCDividendPaymentTransactionType extends CCTransactionType {


    private final AssetService assetService;
    private final AccountAssetService accountAssetService;
    private final Blockchain blockchain;
    private final AssetDividendService assetDividendService;
    private final TransactionValidator transactionValidator;

    @Inject
    public CCDividendPaymentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, AccountAssetService accountAssetService, AssetDividendService assetDividendService, Blockchain blockchain, TransactionValidator transactionValidator) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
        this.assetDividendService = assetDividendService;
        this.accountAssetService = accountAssetService;
        this.blockchain = blockchain;
        this.transactionValidator = transactionValidator;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_DIVIDEND_PAYMENT;
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
    public CCDividendPaymentAttachment parseAttachment(ByteBuffer buffer) {
        return new CCDividendPaymentAttachment(buffer);
    }

    @Override
    public CCDividendPaymentAttachment parseAttachment(JSONObject attachmentData) {
        return new CCDividendPaymentAttachment(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        long assetId = attachment.getAssetId();
        Asset asset = assetService.getAsset(assetId, attachment.getHeight());
        if (asset == null) {
            return true;
        }
        long quantityATU = asset.getQuantityATU() - accountAssetService.getAssetBalanceATU(senderAccount, assetId, attachment.getHeight());
        long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
        if (senderAccount.getUnconfirmedBalanceATM() >= totalDividendPayment) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -totalDividendPayment);
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        accountAssetService.payDividends(senderAccount, transaction.getId(), attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        long assetId = attachment.getAssetId();
        Asset asset = assetService.getAsset(assetId, attachment.getHeight());
        if (asset == null) {
            return;
        }
        long quantityATU = asset.getQuantityATU() - accountAssetService.getAssetBalanceATU(senderAccount, assetId, attachment.getHeight());
        long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), totalDividendPayment);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        Asset asset = assetService.getAsset(attachment.getAssetId(), attachment.getHeight());
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " for dividend payment doesn't exist yet");
        }
        if (asset.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Invalid dividend payment sender " + attachment.getJSONObject());
        }
        AssetDividend lastDividend = assetDividendService.getLastDividend(attachment.getAssetId());
        if (lastDividend != null && lastDividend.getHeight() > blockchain.getHeight() - 60) {
            throw new AplException.NotCurrentlyValidException("Last dividend payment for asset " + Long.toUnsignedString(attachment.getAssetId()) + " was less than 60 blocks ago at " + lastDividend.getHeight() + ", current height is " + blockchain.getHeight() + ", limit is one dividend per 60 blocks");
        }
        long dividendEligibleQuantityATU = asset.getQuantityATU() - accountAssetService.getAssetBalanceATU(transaction.getSenderId(), asset.getId(), attachment.getHeight());
        MathUtils.safeMultiply(attachment.getAmountATMPerATU(), dividendEligibleQuantityATU, transaction);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        if (attachment.getAmountATMPerATU() <= 0) {
            throw new AplException.NotValidException("Invalid dividend payment amount " + attachment.getJSONObject());
        }
        if (attachment.getHeight() > blockchain.getHeight()) {
            throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight() + ", must not exceed current blockchain height " + blockchain.getHeight());
        }
        int finishValidationHeight = transactionValidator.getFinishValidationHeight(transaction, attachment);
        if (attachment.getHeight() <= finishValidationHeight - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK) {
            throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight() + ", must be less than " + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK + " blocks before " + finishValidationHeight);
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        CCDividendPaymentAttachment attachment = (CCDividendPaymentAttachment) transaction.getAttachment();
        return isDuplicate(TransactionTypes.TransactionTypeSpec.CC_DIVIDEND_PAYMENT, Long.toUnsignedString(attachment.getAssetId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

}
