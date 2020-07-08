/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetDelete;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class CCAssetDelete extends ColoredCoins {
    private final AssetService assetService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAssetDelete(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, AccountAssetService accountAssetService) {
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
    public ColoredCoinsAssetDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAssetDelete(buffer);
    }

    @Override
    public ColoredCoinsAssetDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAssetDelete(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(senderAccount, attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        accountAssetService.addToAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        assetService.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        if (attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset identifier: " + attachment.getJSONObject());
        }
        Asset asset = assetService.getAsset(attachment.getAssetId());
        if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
            throw new AplException.NotValidException("Invalid asset delete asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
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
