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

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DGSQuantityChangeAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_DELTA_QUANTITY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_DELTA_QUANTITY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_GOODS;

@Vetoed
public final class DGSQuantityChange extends CreateTransactionHandler {

    private DGSService service = CDI.current().select(DGSService.class).get();

    public DGSQuantityChange() {
        super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION},
            "goods", "deltaQuantity");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = HttpParameterParserUtil.getSenderAccount(req);
        DGSGoods goods = HttpParameterParserUtil.getGoods(service, req);
        if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
            return UNKNOWN_GOODS;
        }

        int deltaQuantity;
        try {
            String deltaQuantityString = Convert.emptyToNull(req.getParameter("deltaQuantity"));
            if (deltaQuantityString == null) {
                return MISSING_DELTA_QUANTITY;
            }
            deltaQuantity = Integer.parseInt(deltaQuantityString);
            if (deltaQuantity > Constants.MAX_DGS_LISTING_QUANTITY || deltaQuantity < -Constants.MAX_DGS_LISTING_QUANTITY) {
                return INCORRECT_DELTA_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELTA_QUANTITY;
        }

        Attachment attachment = new DGSQuantityChangeAttachment(goods.getId(), deltaQuantity);
        return createTransaction(req, account, attachment);

    }

}
