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

public final class DGSDelisting extends CreateTransaction {

    private static class DGSDelistingHolder {
        private static final DGSDelisting INSTANCE = new DGSDelisting();
    }

    public static DGSDelisting getInstance() {
        return DGSDelistingHolder.INSTANCE;
    }

    private DGSDelisting() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION}, "goods");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Account account = ParameterParser.getSenderAccount(req, validate);
        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted() || validate && goods.getSellerId() != account.getId()) {
            return new CreateTransactionRequestData(UNKNOWN_GOODS);
        }
        Attachment attachment = new Attachment.DigitalGoodsDelisting(goods.getId());
        return new CreateTransactionRequestData(attachment, account);
    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        return new CreateTransactionRequestData(new Attachment.DigitalGoodsDelisting(0), null);
    }
}
