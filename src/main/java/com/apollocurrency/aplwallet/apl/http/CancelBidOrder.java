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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_ORDER;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Order;

public final class CancelBidOrder extends CreateTransaction {

    private static class CancelBidOrderHolder {
        private static final CancelBidOrder INSTANCE = new CancelBidOrder();
    }

    public static CancelBidOrder getInstance() {
        return CancelBidOrderHolder.INSTANCE;
    }

    private CancelBidOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "order");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        long orderId = ParameterParser.getUnsignedLong(req, "order", validate);
        Account account = ParameterParser.getSenderAccount(req, validate);
        Order.Bid orderData = Order.Bid.getBidOrder(orderId);
        if (validate && (orderData == null || orderData.getAccountId() != account.getId())) {
            return new CreateTransactionRequestData(UNKNOWN_ORDER);
        }
        Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(orderId);
        return new CreateTransactionRequestData(attachment, account);
    }

}
