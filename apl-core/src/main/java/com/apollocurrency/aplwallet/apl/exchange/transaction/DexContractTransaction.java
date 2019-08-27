package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.Map;

public class DexContractTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_CONTRACT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexContractAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject jsonObject) throws AplException.NotValidException {
        return new DexContractAttachment(jsonObject);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();

        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());
        DexOffer counterOffer = dexService.getOfferByTransactionId(attachment.getCounterOrderId());

       if(attachment.getContractStatus().isStep2()){
           if(offer == null) {
                throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getOrderId());
           }

           if (transaction.getSenderId() != offer.getAccountId() && transaction.getSenderId() != counterOffer.getAccountId()) {
               throw new AplException.NotValidException("Can send tx dex contract only from sender or recipient account.");
           }
       }

        if(counterOffer == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getCounterOrderId());
        }

        if(attachment.getEncryptedSecret() != null && attachment.getEncryptedSecret().length != 64){
            throw new AplException.NotValidException("Encrypted secret is null or length is not right.");
        }


        ExchangeContract contract = dexService.getDexContract(DexContractDBRequest.builder()
                .offerId(attachment.getOrderId())
                .counterOfferId(attachment.getCounterOrderId())
                .build());

        if (attachment.getContractStatus().isStep2() && contract == null) {
            throw new AplException.NotValidException("Don't find contract.");
        }


    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();
        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());
        DexOffer counterOffer = dexService.getOfferByTransactionId(attachment.getCounterOrderId());

        if (attachment.getContractStatus().isStep2() && counterOffer.getStatus().isOpen()) {
            counterOffer.setStatus(OfferStatus.WAITING_APPROVAL);
            dexService.saveOffer(counterOffer);
        }

        ExchangeContract contract = dexService.getDexContract(DexContractDBRequest.builder()
                .offerId(attachment.getOrderId())
                .counterOfferId(attachment.getCounterOrderId())
                .build());

        // contract == null it means, that it's a first step.
        if (contract == null) {
            dexService.saveDexContract(new ExchangeContract(senderAccount.getId(), counterOffer.getAccountId(), attachment));
        } else {
            contract.setCounterOrderId(attachment.getCounterOrderId());
            contract.setCounterTransferTxId(attachment.getCounterTransferTxId());
            contract.setContractStatus(attachment.getContractStatus());

            dexService.saveDexContract(contract);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CONTRACT_TRANSACTION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
        return "DexContract";
    }
}
