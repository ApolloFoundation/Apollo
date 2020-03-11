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

import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.monetary.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataService;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
@Vetoed
public final class GetState extends AbstractAPIRequestHandler {
    private UPnP upnp = CDI.current().select(UPnP.class).get();
    private DGSService service = CDI.current().select(DGSService.class).get();
    private TaggedDataService taggedDataService = CDI.current().select(TaggedDataService.class).get();
    private PrunableMessageService prunableMessageService = CDI.current().select(PrunableMessageService.class).get();

    public GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new GetBlockchainStatus().processRequest(req);

        if ("true".equalsIgnoreCase(req.getParameter("includeCounts")) && apw.checkPassword(req)) {
            response.put("numberOfTransactions", lookupBlockchain().getTransactionCount());
            response.put("numberOfAccounts", lookupAccountPublickKeyService().getCount());
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
            response.put("numberOfGoods", service.getGoodsCount());
            response.put("numberOfPurchases", service.getPurchaseCount());
            response.put("numberOfTags", service.getTagsCount());
            response.put("numberOfPolls", Poll.getCount());
            response.put("numberOfVotes", Vote.getCount());
            response.put("numberOfPrunableMessages", prunableMessageService.getCount());
            response.put("numberOfTaggedData", taggedDataService.getTaggedDataCount());
            response.put("numberOfDataTags", taggedDataService.getDataTagCount());
            response.put("numberOfAccountLeases", lookupAccountLeaseService().getAccountLeaseCount());
            response.put("numberOfActiveAccountLeases", lookupAccountService().getActiveLeaseCount());
            response.put("numberOfShufflings", Shuffling.getCount());
            response.put("numberOfActiveShufflings", Shuffling.getActiveCount());
            response.put("numberOfPhasingOnlyAccounts", PhasingOnly.getCount());
        }
        response.put("numberOfPeers", lookupPeersService().getAllPeers().size());
        response.put("numberOfActivePeers", lookupPeersService().getActivePeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());
        response.put("peerPort", lookupPeersService().myPort);
        response.put("isOffline", propertiesHolder.isOffline());
        response.put("needsAdminPassword", !apw.isDisabledAdminPassword());
        response.put("customLoginWarning", propertiesHolder.customLoginWarning());
        InetAddress externalAddress = upnp.getExternalAddress();
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
