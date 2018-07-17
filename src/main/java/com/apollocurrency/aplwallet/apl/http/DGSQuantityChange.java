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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DELTA_QUANTITY;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DELTA_QUANTITY;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSQuantityChange extends CreateTransaction {

    private static class DGSQuantityChangeHolder {
        private static final DGSQuantityChange INSTANCE = new DGSQuantityChange();
    }

    public static DGSQuantityChange getInstance() {
        return DGSQuantityChangeHolder.INSTANCE;
    }

    private DGSQuantityChange() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "goods", "deltaQuantity");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
            return UNKNOWN_GOODS;
        }

        int deltaQuantity;
        try {
            String deltaQuantityString = Convert.emptyToNull(req.getParameter("deltaQuantity"));
            if (deltaQuantityString == null) {
                return MISSING_DELTA_QUANTITY;
            }
            deltaQuantity = Integer.parseInt(deltaQuantityString);
            if (deltaQuantity > Constants.MAX_DGS_LISTING_QUANTITY || deltaQuantity < -Constants.MAX_DGS_LISTING_QUANTITY) {
                return INCORRECT_DELTA_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELTA_QUANTITY;
        }

        Attachment attachment = new Attachment.DigitalGoodsQuantityChange(goods.getId(), deltaQuantity);
        return createTransaction(req, account, attachment);

    }

}
