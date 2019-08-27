package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;


public class DexCloseOfferTransaction extends DEX {

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
        return new DexCloseOfferAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexCloseOfferAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());

        if (offer.getAccountId() != transaction.getSenderId() ) {
            throw new AplException.NotValidException(JSON.toString(incorrect("orderId", String.format("You can close only your orders."))));
        }

        if (!(offer.getStatus().isWaitingForApproval() || offer.getStatus().isClosed())) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderStatus", String.format("You can close order only in the status WaitingForApproval, but: " + offer.getStatus().name()))));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());
        offer.setStatus(OfferStatus.CLOSED);
        dexService.saveOffer(offer);

        ExchangeContract exchangeContract = dexService.getDexContract(DexContractDBRequest.builder().offerId(offer.getTransactionId()).build());

        if (exchangeContract == null) {
            exchangeContract = dexService.getDexContract(DexContractDBRequest.builder().counterOfferId(offer.getTransactionId()).build());
        }

        Block lastBlock = blockchain.getLastBlock();

        DexTradeEntry dexTradeEntry = DexTradeEntry.builder()
                .transactionID(transaction.getId())
                .senderOfferAmount(exchangeContract.getSender())
                .receiverOfferID(exchangeContract.getRecipient())
                .senderOfferType((byte) offer.getType().ordinal())
                .senderOfferCurrency((byte) offer.getOfferCurrency().ordinal())
                .senderOfferAmount(offer.getOfferAmount())
                .pairCurrency((byte) offer.getPairCurrency().ordinal())
                .pairRate(offer.getPairRate())
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
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CLOSE_OFFER, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
