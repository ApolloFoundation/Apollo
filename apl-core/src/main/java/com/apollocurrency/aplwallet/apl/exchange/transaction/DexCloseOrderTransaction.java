package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;


public class DexCloseOrderTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();
    private BlockchainImpl blockchain = CDI.current().select(BlockchainImpl.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_CLOSE_OFFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexCloseOrderAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexCloseOrderAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        DexOrder order = dexService.getOfferByTransactionId(attachment.getOrderId());

        if (order.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException(JSON.toString(incorrect("orderId", "You can close only your orders.")));
        }

        if (!order.getStatus().isWaitingForApproval()) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderStatus", "You can close order only in the status WaitingForApproval, but: " + order.getStatus().name())));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        DexOrder order = dexService.getOfferByTransactionId(attachment.getOrderId());
        order.setStatus(OrderStatus.CLOSED);
        dexService.saveOrder(order);

        ExchangeContract exchangeContract = dexService.getDexContract(DexContractDBRequest.builder().offerId(order.getTransactionId()).build());

        if (exchangeContract == null) {
            exchangeContract = dexService.getDexContract(DexContractDBRequest.builder().counterOfferId(order.getTransactionId()).build());
        }

        Block lastBlock = blockchain.getLastBlock();

        DexTradeEntry dexTradeEntry = DexTradeEntry.builder()
                .transactionID(transaction.getId())
                .senderOfferID(exchangeContract.getSender())
                .receiverOfferID(exchangeContract.getRecipient())
                .senderOfferType((byte) order.getType().ordinal())
                .senderOfferCurrency((byte) order.getOrderCurrency().ordinal())
                .senderOfferAmount(order.getOrderAmount())
                .pairCurrency((byte) order.getPairCurrency().ordinal())
                .pairRate(order.getPairRate())
                .finishTime(lastBlock.getTimestamp())
                .height(lastBlock.getHeight())
                .build();

        dexService.saveDexTradeEntry(dexTradeEntry);
    }


    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CLOSE_ORDER, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
        return "DexCloseOrder";
    }


}
