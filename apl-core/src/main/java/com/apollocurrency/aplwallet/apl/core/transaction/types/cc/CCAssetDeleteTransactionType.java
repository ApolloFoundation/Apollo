/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetDeleteAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class CCAssetDeleteTransactionType extends CCTransactionType {
    private final AssetService assetService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAssetDeleteTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
        this.accountAssetService = accountAssetService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASSET_DELETE;
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
    public CCAssetDeleteAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new CCAssetDeleteAttachment(buffer);
    }

    @Override
    public CCAssetDeleteAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new CCAssetDeleteAttachment(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(senderAccount.getId(), attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        accountAssetService.addToAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        assetService.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        Asset asset = assetService.getAsset(attachment.getAssetId());
        if (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU()) {
            throw new AplException.NotValidException("Invalid asset delete asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
        }
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(transaction.getSenderId(), attachment.getAssetId());
        if (unconfirmedAssetBalance < attachment.getQuantityATU()) {
            throw new AplException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                + " has not enough " + Long.toUnsignedString(attachment.getAssetId()) + " asset to delete: required "
                + attachment.getQuantityATU() + ", but only has " + unconfirmedAssetBalance);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        if (attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset identifier: " + attachment.getJSONObject());
        }
        if (attachment.getQuantityATU() <= 0) {
            throw new AplException.NotValidException("Invalid asset quantity: " + attachment.getQuantityATU());
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

}
