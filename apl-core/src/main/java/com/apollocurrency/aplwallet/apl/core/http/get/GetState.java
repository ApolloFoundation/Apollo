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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;


@Deprecated
@Vetoed
public final class GetState extends AbstractAPIRequestHandler {
    public GetState() {
        super(new APITag[]{APITag.INFO}, "includeCounts", "adminPassword");
    }

    private ShufflingService shufflingService = CDI.current().select(ShufflingService.class).get();
    private CurrencyExchangeOfferFacade exchangeOfferFacade = CDI.current().select(CurrencyExchangeOfferFacade.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new GetBlockchainStatus().processRequest(req);

        if ("true".equalsIgnoreCase(req.getParameter("includeCounts")) && apw.checkPassword(req)) {
            response.put("numberOfTransactions", lookupBlockchain().getTransactionCount());
            response.put("numberOfAccounts", lookupAccountPublickKeyService().getCount());
            response.put("numberOfAssets", assetService.getCount());
            int askCount = askOrderService.getCount();
            int bidCount = bidOrderService.getCount();
            response.put("numberOfOrders", askCount + bidCount);
            response.put("numberOfAskOrders", askCount);
            response.put("numberOfBidOrders", bidCount);
            response.put("numberOfTrades", tradeService.getCount());
            response.put("numberOfTransfers", lookupAssetTransferService().getCount());
            response.put("numberOfCurrencies", lookupCurrencyService().getCount());
            response.put("numberOfOffers", exchangeOfferFacade.getCurrencyBuyOfferService().getCount());
            response.put("numberOfExchangeRequests", lookupExchangeRequestService().getCount());
            response.put("numberOfExchanges", exchangeService.getCount());
            response.put("numberOfCurrencyTransfers", lookupCurrencyTransferService().getCount());
            response.put("numberOfAliases", aliasService.getCount());
            response.put("numberOfGoods", service.getGoodsCount());
            response.put("numberOfPurchases", service.getPurchaseCount());
            response.put("numberOfTags", service.getTagsCount());
            response.put("numberOfPolls", pollService.getCount());
            response.put("numberOfVotes", pollService.getPollVoteCount());
            response.put("numberOfPrunableMessages", prunableMessageService.getCount());
            response.put("numberOfTaggedData", taggedDataService.getTaggedDataCount());
            response.put("numberOfDataTags", taggedDataService.getDataTagCount());
            response.put("numberOfAccountLeases", lookupAccountLeaseService().getAccountLeaseCount());
            response.put("numberOfActiveAccountLeases", lookupAccountService().getActiveLeaseCount());
            response.put("numberOfShufflings", shufflingService.getShufflingCount());
            response.put("numberOfActiveShufflings", shufflingService.getShufflingActiveCount());
            response.put("numberOfPhasingOnlyAccounts", lookupAccountControlPhasingService().getCount());
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
