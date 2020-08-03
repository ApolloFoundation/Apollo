/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
class CCAskOrderCancellationTransactionType extends ColoredCoinsOrderCancellationTransactionType {
    private final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAskOrderCancellationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService);
        this.askOrderService = askOrderService;
        this.accountAssetService = accountAssetService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_CANCELLATION;
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
    public ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderCancellation(buffer);
    }

    @Override
    public ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderCancellation(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
        AskOrder order = askOrderService.getOrder(attachment.getOrderId());
        askOrderService.removeOrder(attachment.getOrderId());
        if (order != null) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), order.getAssetId(), order.getQuantityATU());
        }
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
        AskOrder ask = askOrderService.getOrder(attachment.getOrderId());
        if (ask == null) {
            throw new AplException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
        }
        if (ask.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(ask.getAccountId()));
        }
    }

}
