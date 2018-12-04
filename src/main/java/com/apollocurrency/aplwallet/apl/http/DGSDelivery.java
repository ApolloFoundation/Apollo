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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.ALREADY_DELIVERED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DGS_GOODS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PURCHASE;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

public final class DGSDelivery extends CreateTransaction {

    private static class DGSDeliveryHolder {
        private static final DGSDelivery INSTANCE = new DGSDelivery();
    }

    public static DGSDelivery getInstance() {
        return DGSDeliveryHolder.INSTANCE;
    }

    private DGSDelivery() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "discountATM", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce", "goodsToEncryptLength",
                "encryptedGoodsLength");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return new CreateTransactionRequestData(INCORRECT_PURCHASE);
        }
        if (! purchase.isPending()) {
            return new CreateTransactionRequestData(ALREADY_DELIVERED);
        }

        String discountValueATM = Convert.emptyToNull(req.getParameter("discountATM"));
        long discountATM = 0;
        try {
            if (discountValueATM != null) {
                discountATM = Long.parseLong(discountValueATM);
            }
        } catch (RuntimeException e) {
            return new CreateTransactionRequestData(INCORRECT_DGS_DISCOUNT);
        }
        if (discountATM < 0
                || discountATM > AplGlobalObjects.getChainConfig().getCurrentConfig().getMaxBalanceATM()
                || discountATM > Math.multiplyExact(purchase.getPriceATM(), (long) purchase.getQuantity())) {
            return new CreateTransactionRequestData(INCORRECT_DGS_DISCOUNT);
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
                    return new CreateTransactionRequestData(INCORRECT_DGS_GOODS);
                }
                goodsBytes = goodsIsText ? Convert.toBytes(plainGoods) : Convert.parseHexString(plainGoods);
            } catch (RuntimeException e) {
                return new CreateTransactionRequestData(INCORRECT_DGS_GOODS);
            }
            byte[] keySeed = ParameterParser.getKeySeed(req, sellerAccount.getId(),broadcast);
            if (keySeed != null) {
                encryptedGoods = buyerAccount.encryptTo(goodsBytes, keySeed, true);
            }
        }

        Attachment attachment = encryptedGoods == null ?
                new Attachment.UnencryptedDigitalGoodsDelivery(purchase.getId(), goodsBytes,
                        goodsIsText, discountATM, Account.getPublicKey(buyerAccount.getId())) :
                new Attachment.DigitalGoodsDelivery(purchase.getId(), encryptedGoods,
                        goodsIsText, discountATM);
        return new CreateTransactionRequestData(attachment, buyerAccount.getId(),sellerAccount, 0);

    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        boolean goodsIsText = !"false".equalsIgnoreCase(req.getParameter("goodsIsText"));
        int encryptedGoodsLength = ParameterParser.getInt(req, "encryptedGoodsLength", 1, Integer.MAX_VALUE, false);
        EncryptedData encryptedGoods;
        if (encryptedGoodsLength != 0) {
            encryptedGoods = new EncryptedData(new byte[encryptedGoodsLength], new byte[32]);
        } else {
            encryptedGoods =  ParameterParser.getEncryptedData(req, "goods");
        }

        if (encryptedGoods == null) {
            int goodsToEncryptLenth = ParameterParser.getInt(req, "goodsToEncryptLength", 1, Integer.MAX_VALUE, false);
            if (goodsToEncryptLenth != 0) {
                encryptedGoods = new EncryptedData(new byte[EncryptedData.getEncryptedDataLength(goodsToEncryptLenth)], new byte[32]);
            } else {
                try {
                    String plainGoods = Convert.nullToEmpty(req.getParameter("goodsToEncrypt"));
                    if (plainGoods.length() == 0) {
                        return new CreateTransactionRequestData(INCORRECT_DGS_GOODS);
                    }
                    byte[] goodsBytes = goodsIsText ? Convert.toBytes(plainGoods) : Convert.parseHexString(plainGoods);
                    encryptedGoods = new EncryptedData(new byte[EncryptedData.getEncryptedDataLength(goodsBytes)], new byte[32]);
                } catch (RuntimeException e) {
                    return new CreateTransactionRequestData(INCORRECT_DGS_GOODS);
                }
            }
        }
        return new CreateTransactionRequestData(new Attachment.DigitalGoodsDelivery(0, encryptedGoods, false, 0), null);

    }
}
