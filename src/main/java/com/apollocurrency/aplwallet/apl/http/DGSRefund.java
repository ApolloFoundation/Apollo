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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.DUPLICATE_REFUND;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.GOODS_NOT_DELIVERED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DGS_REFUND;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.util.Convert;

public final class DGSRefund extends CreateTransaction {

    private static class DGSRefundHolder {
        private static final DGSRefund INSTANCE = new DGSRefund();
    }

    public static DGSRefund getInstance() {
        return DGSRefundHolder.INSTANCE;
    }

    private DGSRefund() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "refundATM");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        Account sellerAccount = ParameterParser.getSenderAccount(req, validate);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (validate && sellerAccount.getId() != purchase.getSellerId()) {
            return new CreateTransactionRequestData(INCORRECT_PURCHASE);
        }
        if (purchase.getRefundNote() != null) {
            return new CreateTransactionRequestData(DUPLICATE_REFUND);
        }
        if (purchase.getEncryptedGoods() == null) {
            return new CreateTransactionRequestData(GOODS_NOT_DELIVERED);
        }

        String refundValueATM = Convert.emptyToNull(req.getParameter("refundATM"));
        long refundATM = 0;
        try {
            if (refundValueATM != null) {
                refundATM = Long.parseLong(refundValueATM);
            }
        } catch (RuntimeException e) {
            return new CreateTransactionRequestData(INCORRECT_DGS_REFUND);
        }
        if (refundATM < 0 || refundATM > Constants.MAX_BALANCE_ATM) {
            return new CreateTransactionRequestData(INCORRECT_DGS_REFUND);
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());

        Attachment attachment = new Attachment.DigitalGoodsRefund(purchase.getId(), refundATM);
        return new CreateTransactionRequestData(attachment, buyerAccount.getId(), sellerAccount, 0);

    }

}
