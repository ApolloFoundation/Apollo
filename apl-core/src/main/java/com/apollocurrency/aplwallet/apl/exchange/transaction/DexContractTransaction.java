package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

        DexOrder order = dexService.getOrder(attachment.getOrderId());
        DexOrder counterOrder = dexService.getOrder(attachment.getCounterOrderId());

        if(attachment.getContractStatus().isStep2()){
            if (order == null) {
                throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getOrderId());
            }

            if (transaction.getSenderId() != order.getAccountId() && transaction.getSenderId() != counterOrder.getAccountId()) {
                throw new AplException.NotValidException("Can send tx dex contract only from sender or recipient account.");
            }
        }

        if (counterOrder == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getCounterOrderId());
        }

        if(attachment.getEncryptedSecret() != null && attachment.getEncryptedSecret().length != 64){
            throw new AplException.NotValidException("Encrypted secret is null or length is not right.");
        }

        if (attachment.getTimeToReply() < Constants.DEX_CONTRACT_TIME_WAITING_TO_REPLY) {
            throw new AplException.NotValidException("Time to reply is less than minimal.");
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
        DexOrder order = dexService.getOrder(attachment.getOrderId());
        DexOrder counterOrder = dexService.getOrder(attachment.getCounterOrderId());

        if (attachment.getContractStatus().isStep2() && counterOrder.getStatus().isOpen()) {
            order.setStatus(OrderStatus.WAITING_APPROVAL);
            dexService.saveOrder(order);

            counterOrder.setStatus(OrderStatus.WAITING_APPROVAL);
            dexService.saveOrder(counterOrder);
        }

        ExchangeContract contract = dexService.getDexContract(DexContractDBRequest.builder()
                .offerId(attachment.getOrderId())
                .counterOfferId(attachment.getCounterOrderId())
                .build());


        // contract == null it means, that it's a first step.
        if (contract == null) {
            contract = new ExchangeContract(transaction.getId(),
                    senderAccount.getId(),
                    counterOrder.getAccountId(),
                    transaction.getBlock().getTimestamp() + attachment.getTimeToReply(),
                    attachment);

            dexService.saveDexContract(contract);
        } else if (attachment.getContractStatus().isStep2() && contract.getContractStatus().isStep1()) {
            contract.setEncryptedSecret(attachment.getEncryptedSecret());
            contract.setSecretHash(attachment.getSecretHash());
            contract.setCounterTransferTxId(attachment.getCounterTransferTxId());
            contract.setContractStatus(ExchangeContractStatus.STEP_2);
            contract.setDeadlineToReply(transaction.getBlock().getTimestamp() + attachment.getTimeToReply());

            //TODO change another orders to the status Open.
            reopenNotMatchedOrders(contract);

            dexService.saveDexContract(contract);
        } else if (attachment.getContractStatus().isStep3() && contract.getContractStatus().isStep2()) {
            contract.setTransferTxId(attachment.getTransferTxId());
            contract.setContractStatus(ExchangeContractStatus.STEP_3);
            contract.setDeadlineToReply(transaction.getBlock().getTimestamp() + attachment.getTimeToReply());

            dexService.saveDexContract(contract);
        } else {
            log.error("Illegal contract state. id: {}, contract status: {}, attachment status: {}", contract.getId(), contract.getContractStatus(), attachment.getContractStatus());
            throw new RuntimeException("Illegal contract state. Perhaps this contract has processed already.");
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


    private void reopenNotMatchedOrders(ExchangeContract contract) {
        List<ExchangeContract> allContracts = dexService.getDexContracts(DexContractDBRequest.builder()
                .counterOfferId(contract.getCounterOrderId())
                .build());

        //Exclude current contract.
        List<ExchangeContract> contractsForReopen = allContracts.stream()
                .filter(c -> !c.getOrderId().equals(contract.getOrderId()))
                .collect(Collectors.toList());

        dexService.closeContracts(contractsForReopen);
    }
}
