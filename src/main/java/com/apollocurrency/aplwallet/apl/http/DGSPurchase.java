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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE_PRICE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE_QUANTITY;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DELIVERY_DEADLINE_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_GOODS;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.util.Convert;

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
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted()) {
            return new CreateTransactionRequestData(UNKNOWN_GOODS);
        }

        int quantity = ParameterParser.getGoodsQuantity(req);
        if (quantity > goods.getQuantity()) {
            return new CreateTransactionRequestData(INCORRECT_PURCHASE_QUANTITY);
        }

        long priceATM = ParameterParser.getPriceATM(req);
        if (priceATM != goods.getPriceATM()) {
            return new CreateTransactionRequestData(INCORRECT_PURCHASE_PRICE);
        }

        String deliveryDeadlineString = Convert.emptyToNull(req.getParameter("deliveryDeadlineTimestamp"));
        if (deliveryDeadlineString == null) {
            return new CreateTransactionRequestData(MISSING_DELIVERY_DEADLINE_TIMESTAMP);
        }
        int deliveryDeadline;
        try {
            deliveryDeadline = Integer.parseInt(deliveryDeadlineString);
            if (deliveryDeadline <= Apl.getEpochTime()) {
                return new CreateTransactionRequestData(INCORRECT_DELIVERY_DEADLINE_TIMESTAMP);
            }
        } catch (NumberFormatException e) {
            return new CreateTransactionRequestData(INCORRECT_DELIVERY_DEADLINE_TIMESTAMP);
        }

        Account buyerAccount = ParameterParser.getSenderAccount(req, validate);
        Account sellerAccount = Account.getAccount(goods.getSellerId());

        Attachment attachment = new Attachment.DigitalGoodsPurchase(goods.getId(), quantity, priceATM,
                deliveryDeadline);
        return new CreateTransactionRequestData(attachment, sellerAccount.getId(),buyerAccount, 0);
    }
}
