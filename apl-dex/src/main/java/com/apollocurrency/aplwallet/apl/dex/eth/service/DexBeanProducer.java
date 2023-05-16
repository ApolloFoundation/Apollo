package com.apollocurrency.aplwallet.apl.dex.eth.service;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static org.web3j.protocol.core.JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;

@Singleton
@Slf4j
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

                    HttpService httpService = new HttpService(fullUrl, createHttpClient());
                    web3j = Web3j.build(httpService);
                }
            }
        }
        return web3j;
    }

    private OkHttpClient createHttpClient() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .writeTimeout(30_000, TimeUnit.MILLISECONDS)
            .readTimeout(30_000, TimeUnit.MILLISECONDS)
            .connectTimeout(30_000, TimeUnit.MILLISECONDS);
        if (log.isDebugEnabled()) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }
        return builder.build();
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
