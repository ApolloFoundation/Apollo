package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
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

        DexOffer offer = dexService.getOfferById(attachment.getOrderId());
        DexOffer counterOffer = dexService.getOfferById(attachment.getCounterOrderId());

        if(offer == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getOrderId());
        }
        if(counterOffer == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getCounterOrderId());
        }

        if(offer.getAccountId() != transaction.getSenderId()){
            throw new AplException.NotValidException("Can create request contract only from your account.");
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();

        dexService.saveDexContract(new ExchangeContract(attachment));
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
