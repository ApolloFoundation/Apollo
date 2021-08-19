/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

/**
 * @author al
 */
@Singleton
public class CCBidOrderPlacementTransactionType extends CCOrderPlacementTransactionType {
    private final OrderMatchService orderMatchService;

    @Inject
    public CCBidOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, OrderMatchService orderMatchService) {
        super(blockchainConfig, accountService, assetService);
        this.orderMatchService = orderMatchService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_PLACEMENT;
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
    public CCBidOrderPlacementAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new CCBidOrderPlacementAttachment(buffer);
    }

    @Override
    public CCBidOrderPlacementAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new CCBidOrderPlacementAttachment(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCBidOrderPlacementAttachment attachment = (CCBidOrderPlacementAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM())) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(),
                -Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        CCBidOrderPlacementAttachment attachment = (CCBidOrderPlacementAttachment) transaction.getAttachment();
        orderMatchService.addBidOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCBidOrderPlacementAttachment attachment = (CCBidOrderPlacementAttachment) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
    }

}
