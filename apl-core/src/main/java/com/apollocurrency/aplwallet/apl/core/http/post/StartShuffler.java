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

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ControlledAccountException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.DuplicateShufflerException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.InvalidRecipientException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ShufflerException;
import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ShufflerLimitException;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffler;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflerService;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflingParticipantService;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflingService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class StartShuffler extends AbstractAPIRequestHandler {

    public StartShuffler() {
        super(new APITag[]{APITag.SHUFFLING}, "secretPhrase", "shufflingFullHash", "recipientSecretPhrase", "recipientPublicKey", "recipientAccount",
                "recipientPassphrase");
    }
    ShufflingService shufflingService = CDI.current().select(ShufflingService.class).get();
    ShufflerService shufflerService = CDI.current().select(ShufflerService.class).get();
    ShufflingParticipantService shufflingParticipantService = CDI.current().select(ShufflingParticipantService.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", true);
        long accountId = ParameterParser.getAccountId(req, vaultAccountName(), false);
        long recipientId = ParameterParser.getAccountId(req, "recipientAccount", false);
        byte[] secretBytes = ParameterParser.getSecretBytes(req, accountId, true);

        byte[] recipientPublicKey = ParameterParser.getPublicKey(req, "recipient", recipientId, true);
        try {
            Shuffler shuffler = shufflerService.addOrGetShuffler(secretBytes, recipientPublicKey, shufflingFullHash);
            return shuffler != null ? JSONData.shuffler(shufflingParticipantService, shuffler, false) : JSON.emptyJSON;
        } catch (ShufflerLimitException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 7);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (DuplicateShufflerException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (InvalidRecipientException e) {
            return JSONResponses.incorrect("recipientPublicKey", e.getMessage());
        } catch (ControlledAccountException e) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 9);
            response.put("errorDescription", e.getMessage());
            return JSON.prepare(response);
        } catch (ShufflerException e) {
            if (e.getCause() instanceof AplException.InsufficientBalanceException) {
                Shuffling shuffling = shufflingService.getShuffling(shufflingFullHash);
                if (shuffling == null) {
                    return JSONResponses.NOT_ENOUGH_FUNDS;
                }
                return JSONResponses.notEnoughHolding(shuffling.getHoldingType());
            }
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
