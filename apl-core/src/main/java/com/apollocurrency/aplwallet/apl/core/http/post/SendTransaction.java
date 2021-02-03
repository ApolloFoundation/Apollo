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

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * Sends a transaction to some peers.
 * Similarly to {@link BroadcastTransaction}, the purpose of {@link SendTransaction} is to support client side
 * signing of transactions.
 * Unlike {@link BroadcastTransaction}, does not validate the transaction and requires adminPassword parameter to avoid
 * abuses. Also does not re-broadcast the transaction and does not store it as unconfirmed transaction.
 * <p>
 * Clients first submit their transaction using {@link CreateTransactionHandler} without providing the secret phrase.<br>
 * In response the client receives the unsigned transaction JSON and transaction bytes.
 * <p>
 * The client then signs and submits the signed transaction using {@link SendTransaction}
 * <p>
 * The default wallet implements this procedure in ars.server.js which you can use as reference.
 * <p>
 * {@link SendTransaction} accepts the following parameters:<br>
 * transactionJSON - JSON representation of the signed transaction<br>
 * transactionBytes - row bytes composing the signed transaction bytes excluding the prunable appendages<br>
 * prunableAttachmentJSON - JSON representation of the prunable appendages<br>
 * <p>
 * Clients can submit either the signed transactionJSON or the signed transactionBytes but not both.<br>
 * In case the client submits transactionBytes for a transaction containing prunable appendages, the client also needs
 * to submit the prunableAttachmentJSON parameter which includes the attachment JSON for the prunable appendages.<br>
 * <p>
 * Prunable appendages are classes implementing the {@link com.apollocurrency.aplwallet.apl} interface.
 */
@Vetoed
public final class SendTransaction extends AbstractAPIRequestHandler {

    public SendTransaction() {
        super(new APITag[]{APITag.TRANSACTIONS}, "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        JSONObject response = new JSONObject();
        try {
            Transaction.Builder builder = HttpParameterParserUtil.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);
            UnconfirmedTransaction transaction = new UnconfirmedTransaction(builder.build(), timeService.systemTimeMillis());
            lookupPeersService().sendToSomePeers(Collections.singletonList(transaction));
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHashString());
        } catch (AplException.NotValidException | RuntimeException e) {
            JSONData.putException(response, e, "Failed to broadcast transaction");
        }
        return response;

    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        //TODO: this is bor bitmart. check for for possible vulnerabilities
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

}
