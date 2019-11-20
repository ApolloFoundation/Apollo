package com.apollocurrency.aplwallet.apl.exchange.service;

import org.web3j.protocol.Web3j;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.web3j.protocol.core.JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;

public class ReceiptProcessorProducer {
    @Inject
    private Web3j web3j;
    @Produces
    public TransactionReceiptProcessor receiptProcessor() {
        return new PollingTransactionReceiptProcessor(web3j, DEFAULT_BLOCK_TIME, DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);
    }
}
