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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Asset;
import com.apollocurrency.aplwallet.apl.Attachment;

public class DividendPayment extends CreateTransaction {

    private static class DividendPaymentHolder {
        private static final DividendPayment INSTANCE = new DividendPayment();
    }

    public static DividendPayment getInstance() {
        return DividendPaymentHolder.INSTANCE;
    }

    private DividendPayment() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "height", "amountATMPerATU");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

            final int height = ParameterParser.getHeight(req);
            final long amountATMPerATU = ParameterParser.getAmountATMPerATU(req);
            final Account account = ParameterParser.getSenderAccount(req);
            final Asset asset = ParameterParser.getAsset(req);
            if (Asset.getAsset(asset.getId(), height) == null) {
                return new CreateTransactionRequestData(JSONResponses.ASSET_NOT_ISSUED_YET);
            }
            final Attachment attachment = new Attachment.ColoredCoinsDividendPayment(asset.getId(), height, amountATMPerATU);
            return new CreateTransactionRequestData(attachment, account, JSONResponses.NOT_ENOUGH_FUNDS);
    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        return new CreateTransactionRequestData(new Attachment.ColoredCoinsDividendPayment(0, 0, 0), null);
    }
}
