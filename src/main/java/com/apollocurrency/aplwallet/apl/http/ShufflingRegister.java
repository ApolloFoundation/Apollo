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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Shuffling;
import org.json.simple.JSONStreamAware;

public final class ShufflingRegister extends CreateTransaction {

    private static class ShufflingRegisterHolder {
        private static final ShufflingRegister INSTANCE = new ShufflingRegister();
    }

    public static ShufflingRegister getInstance() {
        return ShufflingRegisterHolder.INSTANCE;
    }

    private ShufflingRegister() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shufflingFullHash");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", validate);

        Attachment attachment = new Attachment.ShufflingRegistration(shufflingFullHash);

        Account account = ParameterParser.getSenderAccount(req, validate);
        if (account.getControls().contains(Account.ControlType.PHASING_ONLY) && validate) {
            throw new ParameterException(JSONResponses.error("Accounts under phasing only control cannot join a shuffling"));
        }
        Shuffling shuffling = Shuffling.getShuffling(shufflingFullHash);
        JSONStreamAware errorJson;
        if (shuffling == null) {
            errorJson = JSONResponses.NOT_ENOUGH_FUNDS;
        } else {
            errorJson = JSONResponses.notEnoughHolding(shuffling.getHoldingType());

        }
            return new CreateTransactionRequestData(attachment, account, errorJson);
    }
}
