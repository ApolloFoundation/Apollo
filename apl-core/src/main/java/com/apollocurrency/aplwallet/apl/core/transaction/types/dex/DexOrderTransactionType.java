/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.dex;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.rest.service.DexOrderAttachmentFactory;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;

@Singleton
public class DexOrderTransactionType extends DexTransactionType {

    private final TimeService timeService;

    @Inject
    public DexOrderTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DexService dexService, TimeService timeService) {
        super(blockchainConfig, accountService, dexService);
        this.timeService = timeService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DEX_ORDER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public DexOrderAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return DexOrderAttachmentFactory.build(buffer);
    }

    @Override
    public DexOrderAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return DexOrderAttachmentFactory.parse(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();
        if (attachment.getType() == OrderType.SELL.ordinal()) {
            verifyAccountBalanceSufficiency(transaction, attachment.getOrderAmount());
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();

        if (attachment.getOrderCurrency() == attachment.getPairCurrency()) {
            throw new AplException.NotValidException("Invalid Currency codes: " + attachment.getOrderCurrency() + " / " + attachment.getPairCurrency());
        }
        if (attachment.getStatus() != OrderStatus.OPEN.ordinal() && attachment.getStatus() != OrderStatus.PENDING.ordinal()) {
            throw new AplException.NotValidException("Expected status 0 (OPEN) or 1 (PENDING) got, " + attachment.getStatus());
        }

        try {
            DexCurrency.getTypeThrowing(attachment.getOrderCurrency());
            DexCurrency.getTypeThrowing(attachment.getPairCurrency());
            OrderType.getType(attachment.getType());
        } catch (Exception ex) {
            throw new AplException.NotValidException("Invalid dex codes: " + attachment.getOrderCurrency() + " / " + attachment.getPairCurrency() + " / " + attachment.getType());
        }

        if (attachment.getPairRate() <= 0) {
            throw new AplException.NotValidException("pairRate should be more than zero.");
        }
        if (attachment.getOrderAmount() <= 0) {
            throw new AplException.NotValidException("offerAmount should be more than zero.");
        }

        if (attachment instanceof DexOrderAttachmentV2) {
            String address = ((DexOrderAttachmentV2) attachment).getFromAddress();
            if (StringUtils.isBlank(address) || address.length() > Constants.MAX_ADDRESS_LENGTH) {
                throw new AplException.NotValidException("fromAddress should be not null and address length less then " + Constants.MAX_ADDRESS_LENGTH);
            }
        }

        int currentTime = timeService.getEpochTime();
        if (attachment.getFinishTime() <= 0) {
            throw new AplException.NotValidException("finishTime must be a positive value, but got " + attachment.getFinishTime());
        }
        int orderDuration = attachment.getFinishTime() - currentTime;
        if (orderDuration > MAX_ORDER_DURATION_SEC) {
            throw new AplException.NotCurrentlyValidException(String.format("orderDuration %d is not in range [%d-%d]", orderDuration, 1, MAX_ORDER_DURATION_SEC));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        // On the Apl side.
        boolean result = true;
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();
        if (attachment.getType() == OrderType.SELL.ordinal()) {
            if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getOrderAmount()) {
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.DEX_FREEZE_MONEY, transaction.getId(), -attachment.getOrderAmount());
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();

        dexService.saveOrder(new DexOrder(transaction, attachment));
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();
        if (attachment.getType() == OrderType.SELL.ordinal()) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.DEX_FREEZE_MONEY, transaction.getId(), attachment.getOrderAmount());
        }
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public String getName() {
        return "DexOrder";
    }


}
