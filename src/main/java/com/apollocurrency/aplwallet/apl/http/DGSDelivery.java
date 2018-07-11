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
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.ALREADY_DELIVERED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DGS_GOODS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSDelivery extends CreateTransaction {

    private static class DGSDeliveryHolder {
        private static final DGSDelivery INSTANCE = new DGSDelivery();
    }

    public static DGSDelivery getInstance() {
        return DGSDeliveryHolder.INSTANCE;
    }

    private DGSDelivery() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "discountATM", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return INCORRECT_PURCHASE;
        }
        if (! purchase.isPending()) {
            return ALREADY_DELIVERED;
        }

        String discountValueATM = Convert.emptyToNull(req.getParameter("discountATM"));
        long discountATM = 0;
        try {
            if (discountValueATM != null) {
                discountATM = Long.parseLong(discountValueATM);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_DISCOUNT;
        }
        if (discountATM < 0
                || discountATM > Constants.MAX_BALANCE_ATM
                || discountATM > Math.multiplyExact(purchase.getPriceATM(), (long) purchase.getQuantity())) {
            return INCORRECT_DGS_DISCOUNT;
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());
        boolean goodsIsText = !"false".equalsIgnoreCase(req.getParameter("goodsIsText"));
        EncryptedData encryptedGoods = ParameterParser.getEncryptedData(req, "goods");
        byte[] goodsBytes = null;
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));

        if (encryptedGoods == null) {
            try {
                String plainGoods = Convert.nullToEmpty(req.getParameter("goodsToEncrypt"));
                if (plainGoods.length() == 0) {
                    return INCORRECT_DGS_GOODS;
                }
                goodsBytes = goodsIsText ? Convert.toBytes(plainGoods) : Convert.parseHexString(plainGoods);
            } catch (RuntimeException e) {
                return INCORRECT_DGS_GOODS;
            }
            String secretPhrase = ParameterParser.getSecretPhrase(req, broadcast);
            if (secretPhrase != null) {
                encryptedGoods = buyerAccount.encryptTo(goodsBytes, secretPhrase, true);
            }
        }

        Attachment attachment = encryptedGoods == null ?
                new Attachment.UnencryptedDigitalGoodsDelivery(purchase.getId(), goodsBytes,
                        goodsIsText, discountATM, Account.getPublicKey(buyerAccount.getId())) :
                new Attachment.DigitalGoodsDelivery(purchase.getId(), encryptedGoods,
                        goodsIsText, discountATM);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}
