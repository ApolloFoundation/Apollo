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

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.HoldingType;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingCreate extends CreateTransaction {

    private static class ShufflingCreateHolder {
        private static final ShufflingCreate INSTANCE = new ShufflingCreate();
    }

    public static ShufflingCreate getInstance() {
        return ShufflingCreateHolder.INSTANCE;
    }

    private ShufflingCreate() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "holding", "holdingType", "amount", "participantCount", "registrationPeriod");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        HoldingType holdingType = ParameterParser.getHoldingType(req);
        long holdingId = ParameterParser.getHoldingId(req, holdingType);
        long amount = ParameterParser.getLong(req, "amount", 0L, Long.MAX_VALUE, true);
        if (holdingType == HoldingType.APL && amount < Constants.SHUFFLING_DEPOSIT_ATM) {
            return JSONResponses.incorrect("amount", "Minimum shuffling amount is " + Constants.SHUFFLING_DEPOSIT_ATM / Constants.ONE_APL + " " + Constants.COIN_SYMBOL);
        }
        byte participantCount = ParameterParser.getByte(req, "participantCount", Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS,
                Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS, true);
        short registrationPeriod = (short) ParameterParser.getInt(req, "registrationPeriod", 0, Constants.MAX_SHUFFLING_REGISTRATION_PERIOD, true);
        Attachment attachment = new Attachment.ShufflingCreation(holdingId, holdingType, amount, participantCount, registrationPeriod);
        Account account = ParameterParser.getSenderAccount(req);
        if (account.getControls().contains(Account.ControlType.PHASING_ONLY)) {
            return JSONResponses.error("Accounts under phasing only control cannot start a shuffling");
        }
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.notEnoughHolding(holdingType);
        }
    }

}
