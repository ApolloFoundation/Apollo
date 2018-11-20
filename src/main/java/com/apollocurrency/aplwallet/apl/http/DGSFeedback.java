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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.GOODS_NOT_DELIVERED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;

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
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);

        Account buyerAccount = ParameterParser.getSenderAccount(req, validate);
        if (validate && buyerAccount.getId() != purchase.getBuyerId()) {
            return new CreateTransactionRequestData(INCORRECT_PURCHASE);
        }
        if (validate && purchase.getEncryptedGoods() == null) {
            return new CreateTransactionRequestData(GOODS_NOT_DELIVERED);
        }

        Account sellerAccount = Account.getAccount(purchase.getSellerId());
        Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId());
        return new CreateTransactionRequestData(attachment, sellerAccount.getId(), buyerAccount, 0);
    }

}
