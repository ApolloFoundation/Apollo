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

package apl.http;

import apl.AplException;
import apl.Transaction;
import apl.util.Convert;
import apl.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    private static class ParseTransactionHolder {
        private static final ParseTransaction INSTANCE = new ParseTransaction();
    }

    public static ParseTransaction getInstance() {
        return ParseTransactionHolder.INSTANCE;
    }

    private ParseTransaction() {
        super(new APITag[]{APITag.TRANSACTIONS}, "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        Transaction transaction = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON).build();
        JSONObject response = JSONData.unconfirmedTransaction(transaction);
        try {
            transaction.validate();
        } catch (AplException.ValidationException | RuntimeException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            response.put("validate", false);
            JSONData.putException(response, e, "Invalid transaction");
        }
        response.put("verify", transaction.verifySignature());
        return response;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
