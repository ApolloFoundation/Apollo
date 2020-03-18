/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BlockEventSourceProcessor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(BlockEventSourceProcessor.class);
    private final BlockEventSource eventSource;
    private final long accountId;
    private final Blockchain blockchain = CDI.current().select(Blockchain.class).get();
    private DGSService service = CDI.current().select(DGSService.class).get();
    private AccountService accountService = CDI.current().select(AccountServiceImpl.class).get();
    private AccountInfoService accountInfoService = CDI.current().select(AccountInfoServiceImpl.class).get();
    private AccountAssetService accountAssetService = CDI.current().select(AccountAssetServiceImpl.class).get();
    private AccountCurrencyService accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
    private final AliasService aliasService = CDI.current().select(AliasService.class).get();

    public BlockEventSourceProcessor(BlockEventSource eventSource, long accountId) {
        this.eventSource = eventSource;
        this.accountId = accountId;
    }

    @Override
    public void run() {
        Block block = blockchain.getLastBlock();
        Block currentBlock;
        try {
            eventSource.emitEvent(getMessage());
            while (!eventSource.isShutdown()) {


                currentBlock = blockchain.getLastBlock();
                if (currentBlock.getHeight() > block.getHeight()) {

                    eventSource.emitEvent(getMessage());

                    block = currentBlock;
                }
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Unable to send sse event", e);
        }
        LOG.trace("Exit event source {}", eventSource.isShutdown());
    }

    protected JSONObject getBlockchainData(Blockchain blockchain) {
        JSONArray transactionsArray = new JSONArray();
        List<Transaction> list = blockchain.getTransactions(accountId,
                0, (byte) -1, (byte) -1, 0, false,
                false, false, 0, 9, false,
                false, false);
            for (Transaction transaction : list) {
                transactionsArray.add(JSONData.transaction(false, transaction));
            }

        JSONArray purchasesJSON = new JSONArray();

//        try (DbIterator<DigitalGoodsStore.Purchase> purchases = DigitalGoodsStore.Purchase.getPendingSellerPurchases(accountId, 0, 9)) {
        try (DbIterator<DGSPurchase> purchases = service.getPendingSellerPurchases(accountId, 0, 9)) {
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(service, purchases.next()));
            }
        }
        int sellerPurchaseCount = service.getSellerPurchaseCount(accountId, false, false);
        int aliasCount = aliasService.getAccountAliasCount(accountId);
        JSONArray assetJson = new JSONArray();
        List<AccountAsset> accountAssets = accountAssetService.getAssetsByAccount(accountId, -1, 0, 2);
        accountAssets.forEach(accountAsset -> assetJson.add(JSONData.accountAsset(accountAsset, false, true)));

        JSONArray currencyJSON = new JSONArray();
        List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrenciesByAccount(accountId, -1, 0, 2);
        accountCurrencies.forEach(accountCurrency -> currencyJSON.add(JSONData.accountCurrency(accountCurrency, false, true)));

        int messageCount = blockchain.getTransactionCount(accountId, (byte) 1, (byte) 0);
        int currencyCount = accountCurrencyService.getCountByAccount(accountId, -1);
        int assetCount = accountAssetService.getCountByAccount(accountId, -1);
        JSONObject accountJson = putAccount(accountId);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("transactions", transactionsArray);
        jsonObject.put("purchases", purchasesJSON);
        jsonObject.put("purchaseCount", sellerPurchaseCount);
        jsonObject.put("currencyCount", currencyCount);
        jsonObject.put("assetCount", assetCount);
        jsonObject.put("aliasCount", aliasCount);
        jsonObject.put("assets", assetJson);
        jsonObject.put("currencies", currencyJSON);
        jsonObject.put("messageCount", messageCount);
        jsonObject.put("account", accountJson);
        return jsonObject;
    }

    public String getMessage() {
        JSONObject jsonObject = getBlockchainData(blockchain);
        jsonObject.put("block", JSONData.block(blockchain.getLastBlock(), false, false));
        return jsonObject.toJSONString();
    }

    private JSONObject putAccount(long accountId) {
        Account account = accountService.getAccount(accountId);
        JSONObject response = JSONData.accountBalance(account, false);
        JSONData.putAccount(response, "account", account.getId());

        byte[] publicKey = accountService.getPublicKeyByteArray(account.getId());
        if (publicKey != null) {
            response.put("publicKey", Convert.toHexString(publicKey));
        }
        AccountInfo accountInfo = accountInfoService.getAccountInfo(account);
        if (accountInfo != null) {
            response.put("name", Convert.nullToEmpty(accountInfo.getName()));
            response.put("description", Convert.nullToEmpty(accountInfo.getDescription()));
        }
        return response;
    }
}
