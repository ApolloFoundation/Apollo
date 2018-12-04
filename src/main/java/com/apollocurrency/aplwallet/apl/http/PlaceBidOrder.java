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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.NOT_ENOUGH_FUNDS;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Asset;
import com.apollocurrency.aplwallet.apl.Attachment;

public final class PlaceBidOrder extends CreateTransaction {

    private static class PlaceBidOrderHolder {
        private static final PlaceBidOrder INSTANCE = new PlaceBidOrder();
    }

    public static PlaceBidOrder getInstance() {
        return PlaceBidOrderHolder.INSTANCE;
    }

    private PlaceBidOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityATU", "priceATM");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        Asset asset = ParameterParser.getAsset(req, validate);
        long priceATM = ParameterParser.getPriceATM(req, validate);
        long quantityATU = ParameterParser.getQuantityATU(req, validate);
        Account account = ParameterParser.getSenderAccount(req, validate);

        Attachment attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset == null ? 0 : asset.getId(), quantityATU, priceATM);
        return new CreateTransactionRequestData(attachment, account, NOT_ENOUGH_FUNDS);
    }

}
