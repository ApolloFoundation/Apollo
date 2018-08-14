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
import apl.Asset;
import apl.Attachment;
import apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class PlaceBidOrder extends CreateTransaction {

    private static class PlaceBidOrderHolder {
        private static final PlaceBidOrder INSTANCE = new PlaceBidOrder();
    }

    public static PlaceBidOrder getInstance() {
        return PlaceBidOrderHolder.INSTANCE;
    }

    private PlaceBidOrder() {
        super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityATU", "priceATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Asset asset = ParameterParser.getAsset(req);
        long priceATM = ParameterParser.getPriceATM(req);
        long quantityATU = ParameterParser.getQuantityATU(req);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset.getId(), quantityATU, priceATM);
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return NOT_ENOUGH_FUNDS;
        }
    }

}
