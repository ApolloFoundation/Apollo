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

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffler;
import com.apollocurrency.aplwallet.apl.core.exception.ShufflerException;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class StartShuffler extends AbstractAPIRequestHandler {

    public StartShuffler() {
        super(new APITag[]{APITag.SHUFFLING}, "secretPhrase", "shufflingFullHash", "recipientSecretPhrase", "recipientPublicKey", "recipientAccount",
            "recipientPassphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] shufflingFullHash = HttpParameterParserUtil.getBytes(req, "shufflingFullHash", true);
        long accountId = HttpParameterParserUtil.getAccountId(req, vaultAccountName(), false);
        long recipientId = HttpParameterParserUtil.getAccountId(req, "recipientAccount", false);
        byte[] secretBytes = HttpParameterParserUtil.getSecretBytes(req, accountId, true);

        byte[] recipientPublicKey = HttpParameterParserUtil.getPublicKey(req, "recipient", recipientId, true);
        try {
            Shuffler shuffler = lookupShufflerService().addOrGetShuffler(secretBytes, recipientPublicKey, shufflingFullHash);
            return shuffler != null ? JSONData.shuffler(shuffler, false) : JSON.emptyJSON;
        } catch (ShufflerException.ShufflerLimitException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 7);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (ShufflerException.DuplicateShufflerException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (ShufflerException.InvalidRecipientException e) {
            return JSONResponses.incorrect("recipientPublicKey", e.getMessage());
        } catch (ShufflerException.ControlledAccountException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 9);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (ShufflerException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 10);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        }
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
