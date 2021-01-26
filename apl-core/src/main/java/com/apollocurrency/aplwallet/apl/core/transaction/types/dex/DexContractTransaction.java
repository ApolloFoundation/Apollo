/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.dex;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.dex.config.DexConfig;
import com.apollocurrency.aplwallet.apl.dex.core.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DexContractTransaction extends DexTransactionType {

    private final DexConfig dexConfig;

    @Inject
    public DexContractTransaction(BlockchainConfig blockchainConfig, AccountService accountService, DexService dexService, DexConfig dexConfig) {
        super(blockchainConfig, accountService, dexService);
        this.dexConfig = dexConfig;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DEX_CONTRACT;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();

        DexOrder order = dexService.getOrder(attachment.getOrderId());
        DexOrder counterOrder = dexService.getOrder(attachment.getCounterOrderId());
        if (order == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getOrderId());
        }
        if (counterOrder == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + attachment.getCounterOrderId());
        }

        ExchangeContract contract = dexService.getDexContractByOrderAndCounterOrder(attachment.getOrderId(), attachment.getCounterOrderId());

        if (attachment.getContractStatus().isStep1()) {
            if (contract != null) {
                throw new AplException.NotCurrentlyValidException("Contract was created earlier");
            }
            if (counterOrder.getStatus() != OrderStatus.OPEN) {
                throw new AplException.NotCurrentlyValidException("Unable to create contract matched to counterOrder with status " + counterOrder.getStatus() + ", expected status OPEN");
            }
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new AplException.NotCurrentlyValidException("Unable to create contract for order in status " + order.getStatus() + ", expected PENDING");
            }
        }
        if (attachment.getContractStatus().isStep2()) {

            if (contract == null) {
                throw new AplException.NotCurrentlyValidException("Don't find contract.");
            }
            if (contract.getContractStatus() != ExchangeContractStatus.STEP_1) {
                throw new AplException.NotCurrentlyValidException("Incorrect status of contract, expected step1, got " + contract.getContractStatus());
            }
            if (transaction.getSenderId() != counterOrder.getAccountId()) {
                throw new AplException.NotValidException("Contract step2 can be send only by counterOrder creator");
            }
            if (counterOrder.getStatus() != OrderStatus.OPEN) {
                throw new AplException.NotCurrentlyValidException("Unable to create contract matched to counterOrder with status " + counterOrder.getStatus() + ", expected status OPEN");
            }
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new AplException.NotCurrentlyValidException("Unable to create contract for order in status " + order.getStatus() + ", expected PENDING");
            }
        }

        if (attachment.getContractStatus().isStep3()) {
            if (contract == null) {
                throw new AplException.NotCurrentlyValidException("Don't find contract.");
            }
            if (contract.getContractStatus() != ExchangeContractStatus.STEP_2) {
                throw new AplException.NotCurrentlyValidException("Incorrect status of contract, expected step2, got " + contract.getContractStatus());
            }
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();

        if (attachment.getContractStatus().isStep1()) {
            if (attachment.getEncryptedSecret() != null) {
                throw new AplException.NotValidException("Encrypted secret should not present at step1");
            }
            if (attachment.getTransferTxId() != null) {
                throw new AplException.NotValidException("TransferTxId should not present at step1");
            }
            if (attachment.getCounterTransferTxId() != null) {
                throw new AplException.NotValidException("CounterTransferTxId should not present at step1");
            }
        }
        if (attachment.getContractStatus().isStep2()) {

            if (attachment.getEncryptedSecret() == null) {
                throw new AplException.NotValidException("Encrypted secret should be specified");
            }
            if (attachment.getEncryptedSecret().length != 64) {
                throw new AplException.NotValidException("Encrypted secret length is " + attachment.getEncryptedSecret().length + ", expected 64");
            }
            if (attachment.getSecretHash() == null) {
                throw new AplException.NotValidException("Secret hash should be specified");
            }
            if (attachment.getSecretHash().length != 32) {
                throw new AplException.NotValidException("Secret hash length is " + attachment.getSecretHash().length + ", expected 32");
            }
            if (attachment.getCounterTransferTxId() == null) {
                throw new AplException.NotValidException("Counter transfer tx id dont present");
            }
            if (attachment.getTransferTxId() != null) {
                throw new AplException.NotValidException("Transfer tx id not allowed for step2");
            }
        }

        if (attachment.getContractStatus().isStep3()) {
            if (attachment.getEncryptedSecret() != null) {
                throw new AplException.NotValidException("Encrypted secret should not be specified");
            }
            if (attachment.getSecretHash() != null) {
                throw new AplException.NotValidException("Secret hash should not be specified");
            }
            if (attachment.getCounterTransferTxId() != null) {
                throw new AplException.NotValidException("Counter transfer tx id should not be specified");
            }
            if (attachment.getTransferTxId() == null) {
                throw new AplException.NotValidException("Transfer tx id should be specified for step3");
            }
        }

        if (attachment.getTimeToReply() < dexConfig.getMinAtomicSwapDuration()) {
            throw new AplException.NotValidException("Time to reply is less than minimal.");
        }
        if (attachment.getTimeToReply() > Constants.DEX_MAX_CONTRACT_TIME_WAITING_TO_REPLY) {
            throw new AplException.NotValidException("Time to reply is greater than max allowed.");
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

        ExchangeContract contract = dexService.getDexContractByOrderAndCounterOrder(attachment.getOrderId(), attachment.getCounterOrderId());

        // contract == null it means, that it's a first step.
        if (contract == null) {
            contract = new ExchangeContract(transaction.getId(),
                senderAccount.getId(),
                counterOrder.getAccountId(),
                transaction.getBlockTimestamp() + attachment.getTimeToReply(),
                attachment);

            dexService.saveDexContract(contract);
        } else if (attachment.getContractStatus().isStep2() && contract.getContractStatus().isStep1()) {
            order.setStatus(OrderStatus.WAITING_APPROVAL);
            dexService.saveOrder(order);

            counterOrder.setStatus(OrderStatus.WAITING_APPROVAL);
            dexService.saveOrder(counterOrder);

            contract.setEncryptedSecret(attachment.getEncryptedSecret());
            contract.setSecretHash(attachment.getSecretHash());
            contract.setCounterTransferTxId(attachment.getCounterTransferTxId());
            contract.setContractStatus(ExchangeContractStatus.STEP_2);
            contract.setDeadlineToReply(transaction.getBlockTimestamp() + attachment.getTimeToReply());

            //Change another orders to the status Open.
            reopenNotMatchedOrders(contract);

            dexService.saveDexContract(contract);
        } else if (attachment.getContractStatus().isStep3() && contract.getContractStatus().isStep2()) {
            contract.setTransferTxId(attachment.getTransferTxId());
            contract.setContractStatus(ExchangeContractStatus.STEP_3);
            contract.setDeadlineToReply(transaction.getBlockTimestamp() + attachment.getTimeToReply());

            dexService.saveDexContract(contract);
        }

    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DexContractAttachment attachment = (DexContractAttachment) transaction.getAttachment();
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DEX_CONTRACT, Long.toUnsignedString(attachment.getCounterOrderId()), duplicates, true);
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
        List<ExchangeContract> allContracts = dexService.getDexContractsByCounterOrderId(contract.getCounterOrderId());

        //Exclude current contract.
        List<ExchangeContract> contractsForReopen = allContracts.stream()
            .filter(c -> !c.getOrderId().equals(contract.getOrderId()))
            .collect(Collectors.toList());

        dexService.closeContractsReopenOrders(contractsForReopen);
    }
}
