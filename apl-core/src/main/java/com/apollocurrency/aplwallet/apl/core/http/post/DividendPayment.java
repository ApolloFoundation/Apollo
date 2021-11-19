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
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCDividendPaymentAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public class DividendPayment extends CreateTransactionHandler {

    public DividendPayment() {
        super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "height", "amountATMPerATU");
    }

    @Override
    public JSONStreamAware processRequest(final HttpServletRequest request)
        throws AplException {
        final int height = HttpParameterParserUtil.getHeight(request);
        final long amountATMPerATU = HttpParameterParserUtil.getAmountATMPerATU(request);
        final Account account = HttpParameterParserUtil.getSenderAccount(request);
        final Asset asset = HttpParameterParserUtil.getAsset(request);
        AssetService assetService = CDI.current().select(AssetService.class).get();
        if (assetService.getAsset(asset.getId(), height) == null) {
            return JSONResponses.ASSET_NOT_ISSUED_YET;
        }
        final Attachment attachment = new CCDividendPaymentAttachment(asset.getId(), height, amountATMPerATU);
        return this.createTransaction(request, account, attachment);
    }
}
