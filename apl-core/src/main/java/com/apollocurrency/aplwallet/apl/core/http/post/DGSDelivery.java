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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ALREADY_DELIVERED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DGS_GOODS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedDigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONStreamAware;

@Vetoed
public final class DGSDelivery extends CreateTransaction {

    public DGSDelivery() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "discountATM", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

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
                || discountATM > CDI.current().select(BlockchainConfig.class).get().getCurrentConfig().getMaxBalanceATM()
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
            byte[] keySeed = ParameterParser.getKeySeed(req, sellerAccount.getId(),broadcast);
            if (keySeed != null) {
                encryptedGoods = buyerAccount.encryptTo(goodsBytes, keySeed, true);
            }
        }

        Attachment attachment = encryptedGoods == null ?
                new UnencryptedDigitalGoodsDelivery(purchase.getId(), goodsBytes,
                        goodsIsText, discountATM, Account.getPublicKey(buyerAccount.getId())) :
                new DigitalGoodsDelivery(purchase.getId(), encryptedGoods,
                        goodsIsText, discountATM);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}
