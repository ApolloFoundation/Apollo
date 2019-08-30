package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
public class DexTransferMoneyTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();


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
        DexOffer dexOffer = dexService.getOfferByTransactionId(attachment.getOrderId());
        if (dexOffer == null) {
            throw new AplException.NotValidException("Offer does not exist: id - " + attachment.getOrderId());
        }
//        if (dexOffer.getStatus() != OfferStatus.OPEN) {
//            throw new AplException.NotValidException("Wrong state of the offer, expected - " + OfferStatus.OPEN + " , got - " + dexOffer.getStatus());
//        }
        //TODO add contract validation, when A.K. will implement contract handshake
        if (dexOffer.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Unable to send tx for offer with different account id. Expected - " + transaction.getSenderId() + ", got - " + dexOffer.getAccountId());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }


    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
        senderAccount.addToBalanceATM(getLedgerEvent(), transaction.getId(), -attachment.getOfferAmount()); // reduce only balanceATM, assume that unconfirmed balance was reduced earlier and was not recovered yet
        recipientAccount.addToBalanceAndUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), attachment.getOfferAmount());

//        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
//
//        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());
//
//        if(DexCurrencyValidator.haveFreezeOrRefundApl(offer)) {
//            try {
//                dexService.refundAPLFrozenMoney(offer);
//            } catch (AplException.ExecutiveProcessException e) {
//                log.error(e.getMessage(), e);
//                throw new RuntimeException(e);
//            }
//        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexControlOfFrozenMoneyAttachment attachment = (DexControlOfFrozenMoneyAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_TRANSFER_MONEY_TRANSACTION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
