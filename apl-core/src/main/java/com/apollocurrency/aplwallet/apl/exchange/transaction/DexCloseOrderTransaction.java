package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
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
    private PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    private Blockchain blockchain = CDI.current().select(Blockchain.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_CLOSE_ORDER;
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
    public void validateAttachment(Transaction tx) throws AplException.ValidationException {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) tx.getAttachment();
        ExchangeContract dexContract = dexService.getDexContractById(attachment.getContractId());
        if (dexContract == null) {
            throw new AplException.NotCurrentlyValidException("Contract does not exists, id - " + attachment.getContractId());
        }
        if (dexContract.getRecipient() != tx.getSenderId() && dexContract.getSender() != tx.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Account " + tx.getSenderId() + " is not a participant of the contract, id - " + dexContract.getId());
        }
        if (dexContract.getContractStatus() != ExchangeContractStatus.STEP_3) {
            throw new AplException.NotCurrentlyValidException("Wrong contract status, expected STEP_3, got " + dexContract.getContractStatus());
        }
        boolean isSender = dexContract.getSender() == tx.getSenderId();
        long orderId = isSender ? dexContract.getOrderId() : dexContract.getCounterOrderId();
        DexOrder order = dexService.getOrder(orderId);
        if (order == null) {
            throw new AplException.NotCurrentlyValidException("Order with id " + attachment.getContractId() + " does not exists");
        }
        if (order.getAccountId() != tx.getSenderId()) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderId", "You can close only your orders.")));
        }
        if (order.getType() == OrderType.BUY) {
            throw new AplException.NotCurrentlyValidException("APL buy orders are closing automatically");
        }
        if (!order.getStatus().isWaitingForApproval()) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderStatus", "You can close order in the status WaitingForApproval only, but got: " + order.getStatus().name())));
        }
        long transferId = Long.parseUnsignedLong(isSender ? dexContract.getTransferTxId() : dexContract.getCounterTransferTxId());
        Transaction transferTx = blockchain.getTransaction(transferId);
        if (transferTx == null) {
            throw new AplException.NotCurrentlyValidException("Transfer tx was not found: " + transferId);
        }
        if (transferTx.getType() != DEX_TRANSFER_MONEY_TRANSACTION) {
            throw new AplException.NotCurrentlyValidException("Wrong type of transfer tx: " + transferTx.getType());
        }
        if (transferTx.getSenderId() != tx.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Wrong transfer tx sender: " + tx.getSenderId() + ", expected -" + transferTx.getSenderId());
        }
        long transferContractId = ((DexControlOfFrozenMoneyAttachment) transferTx.getAttachment()).getContractId();
        if (transferContractId != attachment.getContractId()) {
            throw new AplException.NotCurrentlyValidException("Trasfer tx " + transferId + " refers to another contract " + transferContractId + ", expected " + attachment.getContractId());
        }
        if (phasingPollService.getPoll(transferId) == null && phasingPollService.getResult(transferId) == null) {
            throw new AplException.NotCurrentlyValidException("Transfer tx " + transferId + " was not phased");
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        ExchangeContract contract = dexService.getDexContractById(attachment.getContractId());
        long orderId = senderAccount.getId() == contract.getSender() ? contract.getOrderId() : contract.getCounterOrderId();
        dexService.closeOrder(orderId);
    }


    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CLOSE_ORDER, Long.toUnsignedString(attachment.getContractId()), duplicates, true);
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
