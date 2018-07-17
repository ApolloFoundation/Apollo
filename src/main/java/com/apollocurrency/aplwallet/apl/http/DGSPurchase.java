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
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE_PRICE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE_QUANTITY;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSPurchase extends CreateTransaction {

    private static class DGSPurchaseHolder {
        private static final DGSPurchase INSTANCE = new DGSPurchase();
    }

    public static DGSPurchase getInstance() {
        return DGSPurchaseHolder.INSTANCE;
    }

    private DGSPurchase() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "goods", "priceATM", "quantity", "deliveryDeadlineTimestamp");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted()) {
            return UNKNOWN_GOODS;
        }

        int quantity = ParameterParser.getGoodsQuantity(req);
        if (quantity > goods.getQuantity()) {
            return INCORRECT_PURCHASE_QUANTITY;
        }

        long priceATM = ParameterParser.getPriceATM(req);
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
            if (deliveryDeadline <= Apl.getEpochTime()) {
                return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
        }

        Account buyerAccount = ParameterParser.getSenderAccount(req);
        Account sellerAccount = Account.getAccount(goods.getSellerId());

        Attachment attachment = new Attachment.DigitalGoodsPurchase(goods.getId(), quantity, priceATM,
                deliveryDeadline);
        try {
            return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }

    }

}
