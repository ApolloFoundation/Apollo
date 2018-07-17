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
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_GOODS;

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
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Account account = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
            return UNKNOWN_GOODS;
        }
        Attachment attachment = new Attachment.DigitalGoodsDelisting(goods.getId());
        return createTransaction(req, account, attachment);
    }

}
