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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.GOODS_NOT_DELIVERED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSFeedback extends CreateTransaction {

    private static class DGSFeedbackHolder {
        private static final DGSFeedback INSTANCE = new DGSFeedback();
    }

    public static DGSFeedback getInstance() {
        return DGSFeedbackHolder.INSTANCE;
    }

    private DGSFeedback() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);

        Account buyerAccount = ParameterParser.getSenderAccount(req);
        if (buyerAccount.getId() != purchase.getBuyerId()) {
            return INCORRECT_PURCHASE;
        }
        if (purchase.getEncryptedGoods() == null) {
            return GOODS_NOT_DELIVERED;
        }

        Account sellerAccount = Account.getAccount(purchase.getSellerId());
        Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId());
        return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
    }

}
