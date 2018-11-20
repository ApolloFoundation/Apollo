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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_VOTE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.POLL_FINISHED;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.Poll;
import com.apollocurrency.aplwallet.apl.util.Convert;


public final class CastVote extends CreateTransaction {

    private static class CastVoteHolder {
        private static final CastVote INSTANCE = new CastVote();
    }

    public static CastVote getInstance() {
        return CastVoteHolder.INSTANCE;
    }

    private CastVote() {
        super(new APITag[]{APITag.VS, APITag.CREATE_TRANSACTION}, "poll", "vote00", "vote01", "vote02");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Poll poll = ParameterParser.getPoll(req);
        if (poll.isFinished()) {
            return new CreateTransactionRequestData(POLL_FINISHED);
        }

        int numberOfOptions = poll.getOptions().length;
        byte[] vote = new byte[numberOfOptions];
        try {
            for (int i = 0; i < numberOfOptions; i++) {
                String voteValue = Convert.emptyToNull(req.getParameter("vote" + (i < 10 ? "0" + i : i)));
                if (voteValue != null) {
                    vote[i] = Byte.parseByte(voteValue);
                    if (vote[i] != Constants.NO_VOTE_VALUE && (vote[i] < poll.getMinRangeValue() || vote[i] > poll.getMaxRangeValue())) {
                        return new CreateTransactionRequestData(INCORRECT_VOTE);
                    }
                } else {
                    vote[i] = Constants.NO_VOTE_VALUE;
                }
            }
        } catch (NumberFormatException e) {
            return new CreateTransactionRequestData(INCORRECT_VOTE);
        }

        Account account = ParameterParser.getSenderAccount(req, validate);
        Attachment attachment = new Attachment.MessagingVoteCasting(poll.getId(), vote);
        return new CreateTransactionRequestData(attachment, account);
    }
}
