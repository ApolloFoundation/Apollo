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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.operation.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.operation.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.operation.order.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.operation.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ORDER;

@Vetoed
public final class CancelBidOrder extends CreateTransaction {
    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService;

    public CancelBidOrder() {
        super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, "order");
    }

    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> lookupBidOrderService() {
        if (bidOrderService == null) {
            this.bidOrderService = CDI.current().select(
                BidOrderServiceImpl.class,
                BidOrderService.Literal.INSTANCE
            ).get();
        }
        return bidOrderService;
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long orderId = HttpParameterParserUtil.getUnsignedLong(req, "order", true);
        Account account = HttpParameterParserUtil.getSenderAccount(req);
        BidOrder orderData = lookupBidOrderService().getOrder(orderId);
        if (orderData == null || orderData.getAccountId() != account.getId()) {
            return UNKNOWN_ORDER;
        }
        Attachment attachment = new ColoredCoinsBidOrderCancellation(orderId);
        return createTransaction(req, account, attachment);
    }
}
