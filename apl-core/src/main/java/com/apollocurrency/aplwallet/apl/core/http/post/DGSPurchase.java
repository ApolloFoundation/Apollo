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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DGSPurchaseAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE_PRICE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE_QUANTITY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_GOODS;

@Vetoed
public final class DGSPurchase extends CreateTransactionHandler {

    private DGSService service = CDI.current().select(DGSService.class).get();

    public DGSPurchase() {
        super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION},
                "goods", "priceATM", "quantity", "deliveryDeadlineTimestamp");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        DGSGoods goods = HttpParameterParserUtil.getGoods(service, req);
        if (goods.isDelisted()) {
            return UNKNOWN_GOODS;
        }

        int quantity = HttpParameterParserUtil.getGoodsQuantity(req);
        if (quantity > goods.getQuantity()) {
            return INCORRECT_PURCHASE_QUANTITY;
        }

        long priceATM = HttpParameterParserUtil.getPriceATM(req);
        if (priceATM != goods.getPriceATM()) {
            return INCORRECT_PURCHASE_PRICE;
        }

        String deliveryDeadlineString = Convert.emptyToNull(req.getParameter("deliveryDeadlineTimestamp"));
        if (deliveryDeadlineString == null) {
            return MISSING_DELIVERY_DEADLINE_TIMESTAMP;
        }
        int deliveryDeadline;
        try {
            deliveryDeadline = Integer.parseInt(deliveryDeadlineString);
            if (deliveryDeadline <= timeService.getEpochTime()) {
                return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
        }

        Account buyerAccount = HttpParameterParserUtil.getSenderAccount(req);
        Account sellerAccount = lookupAccountService().getAccount(goods.getSellerId());

        Attachment attachment = new DGSPurchaseAttachment(goods.getId(), quantity, priceATM, deliveryDeadline);
        return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
    }

}
