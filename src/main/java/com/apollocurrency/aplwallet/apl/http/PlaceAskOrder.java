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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Asset;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.NOT_ENOUGH_ASSETS;

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
