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
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DGSService;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedDigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ALREADY_DELIVERED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DGS_GOODS;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE;

@Vetoed
public final class DGSDelivery extends CreateTransaction {

    private DGSService service = CDI.current().select(DGSService.class).get();

    public DGSDelivery() {
        super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION},
            "purchase", "discountATM", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account sellerAccount = HttpParameterParserUtil.getSenderAccount(req);
        DGSPurchase purchase = HttpParameterParserUtil.getPurchase(service, req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return INCORRECT_PURCHASE;
        }
        if (!purchase.isPending()) {
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

        Account buyerAccount = lookupAccountService().getAccount(purchase.getBuyerId());
        boolean goodsIsText = !"false".equalsIgnoreCase(req.getParameter("goodsIsText"));
        EncryptedData encryptedGoods = HttpParameterParserUtil.getEncryptedData(req, "goods");
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
            byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, sellerAccount.getId(), broadcast);
            if (keySeed != null) {
                encryptedGoods = lookupAccountPublickKeyService().encryptTo(buyerAccount.getId(), goodsBytes, keySeed, true);
            }
        }

        Attachment attachment = encryptedGoods == null ?
            new UnencryptedDigitalGoodsDelivery(purchase.getId(), goodsBytes,
                goodsIsText, discountATM, lookupAccountService().getPublicKeyByteArray(buyerAccount.getId())) :
            new DigitalGoodsDelivery(purchase.getId(), encryptedGoods,
                goodsIsText, discountATM);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}
