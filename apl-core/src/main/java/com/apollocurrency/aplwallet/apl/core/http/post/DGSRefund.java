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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.DUPLICATE_REFUND;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.GOODS_NOT_DELIVERED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DGS_REFUND;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PURCHASE;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsRefund;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return INCORRECT_PURCHASE;
        }
        if (purchase.getRefundNote() != null) {
            return DUPLICATE_REFUND;
        }
        if (purchase.getEncryptedGoods() == null) {
            return GOODS_NOT_DELIVERED;
        }

        String refundValueATM = Convert.emptyToNull(req.getParameter("refundATM"));
        long refundATM = 0;
        try {
            if (refundValueATM != null) {
                refundATM = Long.parseLong(refundValueATM);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_REFUND;
        }
        if (refundATM < 0 || refundATM > CDI.current().select(BlockchainConfig.class).get().getCurrentConfig().getMaxBalanceATM()) {
            return INCORRECT_DGS_REFUND;
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());

        Attachment attachment = new DigitalGoodsRefund(purchase.getId(), refundATM);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}
