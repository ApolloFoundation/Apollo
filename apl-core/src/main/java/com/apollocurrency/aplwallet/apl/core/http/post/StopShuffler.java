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

import com.apollocurrency.aplwallet.apl.core.app.Shuffler;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflerService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class StopShuffler extends AbstractAPIRequestHandler {

    public StopShuffler() {
        super(new APITag[]{APITag.SHUFFLING}, "shufflingFullHash", "secretPhrase", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        ShufflerService shufflerService = lookupShufflerService();
        byte[] shufflingFullHash = HttpParameterParserUtil.getBytes(req, "shufflingFullHash", false);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);
        JSONObject response = new JSONObject();
        if (keySeed != null) {
            if (accountId != 0 && AccountService.getId(Crypto.getPublicKey(keySeed)) != accountId) {
                return JSONResponses.INCORRECT_ACCOUNT;
            }
            accountId = AccountService.getId(Crypto.getPublicKey(keySeed));
            if (shufflingFullHash.length == 0) {
                return JSONResponses.missing("shufflingFullHash");
            }
            Shuffler shuffler = shufflerService.stopShuffler(accountId, shufflingFullHash);
            response.put("stoppedShuffler", shuffler != null);
        } else {
            apw.verifyPassword(req);
            if (accountId != 0 && shufflingFullHash.length != 0) {
                Shuffler shuffler = shufflerService.stopShuffler(accountId, shufflingFullHash);
                response.put("stoppedShuffler", shuffler != null);
            } else if (accountId == 0 && shufflingFullHash.length == 0) {
                shufflerService.stopAllShufflers();
                response.put("stoppedAllShufflers", true);
            } else if (accountId != 0) {
                return JSONResponses.missing("shufflingFullHash");
            } else if (shufflingFullHash.length != 0) {
                return JSONResponses.missing("account");
            }
        }
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }

    @Override
    protected boolean is2FAProtected() {
        return true;
    }
}
