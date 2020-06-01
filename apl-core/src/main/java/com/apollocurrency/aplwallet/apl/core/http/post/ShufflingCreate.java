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

import com.apollocurrency.aplwallet.apl.core.model.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.operation.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class ShufflingCreate extends CreateTransaction {

    public ShufflingCreate() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
            "holding", "holdingType", "amount", "participantCount", "registrationPeriod");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        HoldingType holdingType = HttpParameterParserUtil.getHoldingType(req);
        long holdingId = HttpParameterParserUtil.getHoldingId(req, holdingType);
        long amount = HttpParameterParserUtil.getLong(req, "amount", 0L, Long.MAX_VALUE, true);
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        if (holdingType == HoldingType.APL && amount < blockchainConfig.getShufflingDepositAtm()) {
            return JSONResponses.incorrect("amount",
                "Minimum shuffling amount is " + blockchainConfig.getShufflingDepositAtm() / Constants.ONE_APL + " " + blockchainConfig.getCoinSymbol());
        }
        byte participantCount = HttpParameterParserUtil.getByte(req, "participantCount", Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS,
            Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS, true);
        short registrationPeriod = (short) HttpParameterParserUtil.getInt(req, "registrationPeriod", 0, Constants.MAX_SHUFFLING_REGISTRATION_PERIOD, true);
        Attachment attachment = new ShufflingCreation(holdingId, holdingType, amount, participantCount, registrationPeriod);
        Account account = HttpParameterParserUtil.getSenderAccount(req);
        if (account.getControls().contains(AccountControlType.PHASING_ONLY)) {
            return JSONResponses.error("Accounts under phasing only control cannot start a shuffling");
        }
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.notEnoughHolding(holdingType);
        }
    }
}
