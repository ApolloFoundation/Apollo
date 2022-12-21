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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRUNED_TRANSACTION;

@Vetoed
public final class GetPrunableMessage extends AbstractAPIRequestHandler {

    public GetPrunableMessage() {
        super(new APITag[]{APITag.MESSAGES}, "transaction", "secretPhrase", "sharedKey", "retrieve", "account", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long transactionId = HttpParameterParserUtil.getUnsignedLong(req, "transaction", true);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);
        byte[] sharedKey = HttpParameterParserUtil.getBytes(req, "sharedKey", false);
        if (sharedKey.length != 0 && keySeed != null) {
            return JSONResponses.either("secretPhrase", "sharedKey", "passphrase & account");
        }
        boolean retrieve = "true".equalsIgnoreCase(req.getParameter("retrieve"));
        PrunableMessage prunableMessage = prunableMessageService.get(transactionId);
        if (prunableMessage == null && retrieve) {
            if (prunableRestorationService.restorePrunedTransaction(transactionId) == null) {
                return PRUNED_TRANSACTION;
            }
            prunableMessage = prunableMessageService.get(transactionId);
        }
        if (prunableMessage != null) {
            return JSONData.prunableMessage(prunableMessageService, prunableMessage, keySeed, sharedKey);
        }
        return JSON.emptyJSON;
    }

}
