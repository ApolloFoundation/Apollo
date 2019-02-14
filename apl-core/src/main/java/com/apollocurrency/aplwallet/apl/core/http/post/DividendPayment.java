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

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

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
    public JSONStreamAware processRequest(final HttpServletRequest request)
            throws AplException
    {
        final int height = ParameterParser.getHeight(request);
        final long amountATMPerATU = ParameterParser.getAmountATMPerATU(request);
        final Account account = ParameterParser.getSenderAccount(request);
        final Asset asset = ParameterParser.getAsset(request);
        if (Asset.getAsset(asset.getId(), height) == null) {
            return JSONResponses.ASSET_NOT_ISSUED_YET;
        }
        final Attachment attachment = new ColoredCoinsDividendPayment(asset.getId(), height, amountATMPerATU);
        try {
            return this.createTransaction(request, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }
    }

}
