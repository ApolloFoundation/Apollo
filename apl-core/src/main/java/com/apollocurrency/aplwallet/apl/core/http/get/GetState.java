/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountRestrictions;
import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.monetary.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import javax.enterprise.inject.spi.CDI;

public final class GetState extends AbstractAPIRequestHandler {
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get(); 
    private static class GetStateHolder {
        private static final GetState INSTANCE = new GetState();
    }

    public static GetState getInstance() {
        return GetStateHolder.INSTANCE;
    }

    private GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = GetBlockchainStatus.getInstance().processRequest(req);

        if ("true".equalsIgnoreCase(req.getParameter("includeCounts")) && API.checkPassword(req)) {
            response.put("numberOfTransactions", lookupBlockchain().getTransactionCount());
            response.put("numberOfAccounts", Account.getCount());
            response.put("numberOfAssets", Asset.getCount());
            int askCount = Order.Ask.getCount();
            int bidCount = Order.Bid.getCount();
            response.put("numberOfOrders", askCount + bidCount);
            response.put("numberOfAskOrders", askCount);
            response.put("numberOfBidOrders", bidCount);
            response.put("numberOfTrades", Trade.getCount());
            response.put("numberOfTransfers", AssetTransfer.getCount());
	        response.put("numberOfCurrencies", Currency.getCount());
    	    response.put("numberOfOffers", CurrencyBuyOffer.getCount());
            response.put("numberOfExchangeRequests", ExchangeRequest.getCount());
        	response.put("numberOfExchanges", Exchange.getCount());
        	response.put("numberOfCurrencyTransfers", CurrencyTransfer.getCount());
            response.put("numberOfAliases", Alias.getCount());
            response.put("numberOfGoods", DigitalGoodsStore.Goods.getCount());
            response.put("numberOfPurchases", DigitalGoodsStore.Purchase.getCount());
            response.put("numberOfTags", DigitalGoodsStore.Tag.getCount());
            response.put("numberOfPolls", Poll.getCount());
            response.put("numberOfVotes", Vote.getCount());
            response.put("numberOfPrunableMessages", PrunableMessage.getCount());
            response.put("numberOfTaggedData", TaggedData.getCount());
            response.put("numberOfDataTags", TaggedData.Tag.getTagCount());
            response.put("numberOfAccountLeases", AccountLeaseTable.getAccountLeaseCount());
            response.put("numberOfActiveAccountLeases", Account.getActiveLeaseCount());
            response.put("numberOfShufflings", Shuffling.getCount());
            response.put("numberOfActiveShufflings", Shuffling.getActiveCount());
            response.put("numberOfPhasingOnlyAccounts", PhasingOnly.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfActivePeers", Peers.getActivePeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());
        response.put("peerPort", Peers.getDefaultPeerPort());
        response.put("isOffline", propertiesHolder.isOffline());
        response.put("needsAdminPassword", !API.disableAdminPassword);
        response.put("customLoginWarning", propertiesHolder.customLoginWarning());
        InetAddress externalAddress = UPnP.getInstance().getExternalAddress();
        if (externalAddress != null) {
            response.put("upnpExternalAddress", externalAddress.getHostAddress());
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
