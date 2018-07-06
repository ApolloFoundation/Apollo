/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.http;

import apl.Account;
import apl.Asset;
import apl.Attachment;
import apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class PlaceAskOrder extends CreateTransaction {

    private static class PlaceAskOrderHolder {
        private static final PlaceAskOrder INSTANCE = new PlaceAskOrder();
    }

    public static PlaceAskOrder getInstance() {
        return PlaceAskOrderHolder.INSTANCE;
    }

    private PlaceAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityATU", "priceATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Asset asset = ParameterParser.getAsset(req);
        long priceATM = ParameterParser.getPriceATM(req);
        long quantityATU = ParameterParser.getQuantityATU(req);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset.getId(), quantityATU, priceATM);
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return NOT_ENOUGH_ASSETS;
        }
    }

}
