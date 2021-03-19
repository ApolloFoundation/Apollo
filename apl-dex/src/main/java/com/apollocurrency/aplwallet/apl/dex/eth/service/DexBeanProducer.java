package com.apollocurrency.aplwallet.apl.dex.eth.service;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.web3j.protocol.core.JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;

@Singleton
public class DexBeanProducer {
    private final PropertiesHolder propertiesHolder;
    private volatile Web3j web3j;
    private volatile TransactionReceiptProcessor transactionReceiptProcessor;

    @Inject
    public DexBeanProducer(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public Web3j web3j() {
        if (web3j == null) {
            synchronized (this) {
                if (web3j == null) {
                    String ethNodeUrl = propertiesHolder.getStringProperty("apl.eth.node.url");
                    String ethNodePort = propertiesHolder.getStringProperty("apl.eth.node.port");
                    //TODO move HttpService config to config files.
                    String fullUrl = ethNodeUrl;

                    if (!StringUtils.isBlank(ethNodePort)) {
                        fullUrl = fullUrl.concat(":" + ethNodePort);
                    }

                    web3j = Web3j.build(new HttpService(fullUrl));
                }
            }
        }
        return web3j;
    }

    public TransactionReceiptProcessor receiptProcessor() {
        if (transactionReceiptProcessor == null) {
            synchronized (this) {
                if (transactionReceiptProcessor == null) {
                    transactionReceiptProcessor = new PollingTransactionReceiptProcessor(web3j(), DEFAULT_BLOCK_TIME, DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);
                }
            }
        }
        return transactionReceiptProcessor;
    }
}
