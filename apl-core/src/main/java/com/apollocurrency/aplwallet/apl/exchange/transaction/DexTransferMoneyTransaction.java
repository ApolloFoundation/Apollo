/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.operation.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
public class DexTransferMoneyTransaction extends DEX {

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_TRANSFER_MONEY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DEX_TRANSFER_MONEY;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexControlOfFrozenMoneyAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexControlOfFrozenMoneyAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        // IMPORTANT! Validation should restrict sending this transaction without money freezing and out of the dex scope
        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
        DexService dexService = lookupDexService();
        ExchangeContract dexContract = dexService.getDexContractById(attachment.getContractId());

        if (dexContract == null) {
            throw new AplException.NotCurrentlyValidException("Contract does not exist: id - " + attachment.getContractId());
        }
        if (dexContract.getRecipient() != transaction.getSenderId() && dexContract.getSender() != transaction.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Account" + transaction.getSenderId() + " is not a party of the contract. Expected - " + dexContract.getRecipient() + " or  " + dexContract.getSender());
        }
        boolean isSender = dexContract.getSender() == transaction.getSenderId();
        long recipient = isSender ? dexContract.getRecipient() : dexContract.getSender();
        if (recipient != transaction.getRecipientId()) {
            throw new AplException.NotCurrentlyValidException("Tx recipient differs from account, specified in the contract");
        }
        long transactionId = Convert.parseUnsignedLong(isSender ? dexContract.getTransferTxId() : dexContract.getCounterTransferTxId());
        if (transactionId == 0) {
            throw new AplException.NotCurrentlyValidException("Contract transaction was not pre confirmed or missing");
        }
        if (transaction.getId() != transactionId) {
            throw new AplException.NotCurrentlyValidException("Transaction was not registered in the contract. ");
        }
        long recipientOrderId = isSender ? dexContract.getCounterOrderId() : dexContract.getOrderId();
        DexOrder recipientOrder = dexService.getOrder(recipientOrderId);
        if (recipientOrder == null) {
            throw new AplException.NotCurrentlyValidException("Contract: " + dexContract.getId() + " refer to non-existent order: " + recipientOrderId);
        }
        if (recipientOrder.getAccountId() != transaction.getRecipientId()) {
            throw new AplException.NotCurrentlyValidException("Order" + recipientOrderId + " should belong to the account: " + transaction.getRecipientId());
        }
        if (recipientOrder.getStatus() != OrderStatus.WAITING_APPROVAL) {
            throw new AplException.NotCurrentlyValidException("Inconsistent order state for id: " + recipientOrder + ", expected - " + OrderStatus.WAITING_APPROVAL + ", got " + recipientOrder.getStatus());
        }
        if (recipientOrder.getType() != OrderType.BUY) {
            throw new AplException.NotCurrentlyValidException("Required BUY type for order " + recipientOrderId);
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }


    @Override
    public void applyAttachment(Transaction tx, Account sender, Account recipient) {
        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) tx.getAttachment();
        lookupAccountService().addToBalanceATM(sender, getLedgerEvent(), tx.getId(), -attachment.getOfferAmount()); // reduce only balanceATM, assume that unconfirmed balance was reduced earlier and was not recovered yet
        lookupAccountService().addToBalanceAndUnconfirmedBalanceATM(recipient, getLedgerEvent(), tx.getId(), attachment.getOfferAmount());

        DexService dexService = lookupDexService();
        ExchangeContract dexContract = dexService.getDexContractById(attachment.getContractId());

        long orderToClose = dexContract.getSender() == sender.getId() ? dexContract.getCounterOrderId() : dexContract.getOrderId(); // close order which was approved
        dexService.closeOrder(orderToClose);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_TRANSFER_MONEY_TRANSACTION, Long.toUnsignedString(attachment.getContractId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }

    @Override
    public String getName() {
        return "DexTransferMoney";
    }
}
