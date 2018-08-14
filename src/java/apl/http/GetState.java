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
 * Copyright © 2018 Apollo Foundation
 */

package apl.http;

import apl.Account;
import apl.AccountRestrictions;
import apl.Alias;
import apl.Asset;
import apl.AssetTransfer;
import apl.Constants;
import apl.Currency;
import apl.CurrencyBuyOffer;
import apl.CurrencyTransfer;
import apl.DigitalGoodsStore;
import apl.Exchange;
import apl.ExchangeRequest;
import apl.Generator;
import apl.Apl;
import apl.Order;
import apl.Poll;
import apl.PrunableMessage;
import apl.Shuffling;
import apl.TaggedData;
import apl.Trade;
import apl.Vote;
import apl.peer.Peers;
import apl.util.UPnP;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

public final class GetState extends APIServlet.APIRequestHandler {

    private static class GetStateHolder {
        private static final GetState INSTANCE = new GetState();
    }

    public static GetState getInstance() {
        return GetStateHolder.INSTANCE;
    }

    private GetState() {
        super(new APITag[]{APITag.INFO}, "includeCounts", "adminPassword");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = GetBlockchainStatus.getInstance().processRequest(req);

        if ("true".equalsIgnoreCase(req.getParameter("includeCounts")) && API.checkPassword(req)) {
            response.put("numberOfTransactions", Apl.getBlockchain().getTransactionCount());
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
            response.put("numberOfAccountLeases", Account.getAccountLeaseCount());
            response.put("numberOfActiveAccountLeases", Account.getActiveLeaseCount());
            response.put("numberOfShufflings", Shuffling.getCount());
            response.put("numberOfActiveShufflings", Shuffling.getActiveCount());
            response.put("numberOfPhasingOnlyAccounts", AccountRestrictions.PhasingOnly.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfActivePeers", Peers.getActivePeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());
        response.put("peerPort", Peers.getDefaultPeerPort());
        response.put("isOffline", Constants.isOffline);
        response.put("needsAdminPassword", !API.disableAdminPassword);
        response.put("customLoginWarning", Constants.customLoginWarning);
        InetAddress externalAddress = UPnP.getExternalAddress();
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
