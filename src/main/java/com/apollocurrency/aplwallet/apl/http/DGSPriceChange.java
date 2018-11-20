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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_GOODS;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;

public final class DGSPriceChange extends CreateTransaction {

    private static class DGSPriceChangeHolder {
        private static final DGSPriceChange INSTANCE = new DGSPriceChange();
    }

    public static DGSPriceChange getInstance() {
        return DGSPriceChangeHolder.INSTANCE;
    }

    private DGSPriceChange() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "goods", "priceATM");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Account account = ParameterParser.getSenderAccount(req, validate);
        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        long priceATM = ParameterParser.getPriceATM(req);
        if (goods.isDelisted() || validate && goods.getSellerId() != account.getId()) {
            return new CreateTransactionRequestData(UNKNOWN_GOODS);
        }
        Attachment attachment = new Attachment.DigitalGoodsPriceChange(goods.getId(), priceATM);
        return new CreateTransactionRequestData(attachment, account);
    }

}
