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


import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_TRANSACTION_FULL_HASH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.TOO_MANY_PHASING_VOTES;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_TRANSACTION_FULL_HASH;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.PhasingPoll;
import com.apollocurrency.aplwallet.apl.util.Convert;

public class ApproveTransaction extends CreateTransaction {
    public static ApproveTransaction getInstance() {
        return ApproveTransactionHolder.INSTANCE;
    }

    private static class ApproveTransactionHolder {
        private static final ApproveTransaction INSTANCE = new ApproveTransaction();
    }
    private ApproveTransaction() {
        super(new APITag[]{APITag.CREATE_TRANSACTION, APITag.PHASING}, "transactionFullHash", "transactionFullHash", "transactionFullHash",
                "revealedSecret", "revealedSecretIsText");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        String[] phasedTransactionValues = req.getParameterValues("transactionFullHash");

        if (phasedTransactionValues == null || phasedTransactionValues.length == 0) {
            return new CreateTransactionRequestData(MISSING_TRANSACTION_FULL_HASH);
        }

        if (phasedTransactionValues.length > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
            return new CreateTransactionRequestData(TOO_MANY_PHASING_VOTES);
        }

        List<byte[]> phasedTransactionFullHashes = new ArrayList<>(phasedTransactionValues.length);
        for (String phasedTransactionValue : phasedTransactionValues) {
            byte[] hash = Convert.parseHexString(phasedTransactionValue);
            PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
            if (phasingPoll == null) {
                return new CreateTransactionRequestData(UNKNOWN_TRANSACTION_FULL_HASH);
            }
            phasedTransactionFullHashes.add(hash);
        }

        byte[] secret;
        String secretValue = Convert.emptyToNull(req.getParameter("revealedSecret"));
        if (secretValue != null) {
            boolean isText = "true".equalsIgnoreCase(req.getParameter("revealedSecretIsText"));
            secret = isText ? Convert.toBytes(secretValue) : Convert.parseHexString(secretValue);
        } else {
            String secretText = Convert.emptyToNull(req.getParameter("revealedSecretText"));
            if (secretText != null) {
                secret = Convert.toBytes(secretText);
            } else {
                secret = Convert.EMPTY_BYTE;
            }
        }
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingPhasingVoteCasting(phasedTransactionFullHashes, secret);
        return new CreateTransactionRequestData(attachment, account);
    }
}
