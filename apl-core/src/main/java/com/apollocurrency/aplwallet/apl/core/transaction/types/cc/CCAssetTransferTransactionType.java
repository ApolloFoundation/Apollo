/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class CCAssetTransferTransactionType extends ColoredCoinsTransactionType {


    private final AccountAssetService accountAssetService;
    private final AssetService assetService;
    private final AssetTransferService assetTransferService;

    @Inject
    public CCAssetTransferTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountAssetService accountAssetService, AssetService assetService, AssetTransferService assetTransferService) {
        super(blockchainConfig, accountService);
        this.accountAssetService = accountAssetService;
        this.assetService = assetService;
        this.assetTransferService = assetTransferService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASSET_TRANSFER;
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
    public ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAssetTransfer(buffer);
    }

    @Override
    public ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAssetTransfer(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(senderAccount, attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        accountAssetService.addToAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        if (recipientAccount.getId() == GenesisImporter.CREATOR_ID) {
            assetService.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
        } else {
            accountAssetService.addToAssetAndUnconfirmedAssetBalanceATU(recipientAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
            assetTransferService.addAssetTransfer(transaction, attachment);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        Asset asset = assetService.getAsset(attachment.getAssetId());
        if (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU()) {
            throw new AplException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        if (transaction.getAmountATM() != 0 || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset transfer amount or asset: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Asset transfer to Genesis not allowed, " + "use asset delete attachment instead");
        }
        if (attachment.getQuantityATU() <= 0) {
            throw new AplException.NotValidException("Invalid asset quantity: " + attachment.getQuantityATU());
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

}
