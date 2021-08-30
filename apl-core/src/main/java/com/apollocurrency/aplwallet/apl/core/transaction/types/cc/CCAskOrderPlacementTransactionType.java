/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

/**
 * @author al
 */
@Singleton
public class CCAskOrderPlacementTransactionType extends CCOrderPlacementTransactionType {
    private final OrderMatchService orderMatchService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAskOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, OrderMatchService orderMatchService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService, assetService);
        this.orderMatchService = orderMatchService;
        this.accountAssetService = accountAssetService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_PLACEMENT;
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
    public ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderPlacement(buffer);
    }

    @Override
    public ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderPlacement(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(senderAccount, attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        orderMatchService.addAskOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

}
