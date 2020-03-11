package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.web3j.protocol.core.JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;

@Singleton
public class DexBeanProducer {

    private PropertiesHolder propertiesHolder;
    private static Web3j web3j;
    private static TransactionReceiptProcessor transactionReceiptProcessor;

    @Inject
    public DexBeanProducer(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public synchronized Web3j web3j(){
        if(web3j != null){
            return web3j;
        }

        String ethNodeUrl = propertiesHolder.getStringProperty("apl.eth.node.url");
        String ethNodePort = propertiesHolder.getStringProperty("apl.eth.node.port");
        //TODO move HttpService config to config files.
        String fullUrl = ethNodeUrl;

        if(!StringUtils.isBlank(ethNodePort)) {
            fullUrl = fullUrl.concat(":" + ethNodePort);
        }

        Web3j web3 = Web3j.build(new HttpService(fullUrl));  // defaults to http://localhost:8545/
        web3j = web3;
        return web3;
    }

    public synchronized TransactionReceiptProcessor receiptProcessor() {
        if(transactionReceiptProcessor != null){
            return transactionReceiptProcessor;
        }

        transactionReceiptProcessor = new PollingTransactionReceiptProcessor(web3j(), DEFAULT_BLOCK_TIME, DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);
        return transactionReceiptProcessor;
    }
}
