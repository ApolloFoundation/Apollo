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
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.PhasingParams;
import org.json.simple.JSONStreamAware;
/**
 * Sets an account control that blocks transactions unless they are phased with certain parameters
 * 
 * <p>
 * Parameters
 * <ul>
 * <li>controlVotingModel - The expected voting model of the phasing. Possible values: 
 *  <ul>
 *  <li>NONE(-1) - the phasing control is removed</li>
 *  <li>ACCOUNT(0) - only by-account voting is allowed</li>
 *  <li>ATM(1) - only balance voting is allowed</li>
 *  <li>ASSET(2) - only asset voting is allowed</li>
 *  <li>CURRENCY(3) - only currency voting is allowed</li>
 *  </ul>
 * </li>
 * <li>controlQuorum - The expected quorum.</li>
 * <li>controlMinBalance - The expected minimum balance</li>
 * <li>controlMinBalanceModel - The expected minimum balance model. Possible values:
 * <ul>
 *  <li>NONE(0) No minimum balance restriction</li>
 *  <li>ATM(1) Apl balance threshold</li>
 *  <li>ASSET(2) Asset balance threshold</li>
 *  <li>CURRENCY(3) Currency balance threshold</li>
 * </ul>
 * </li>
 * <li>controlHolding - The expected holding ID - asset ID or currency ID.</li>
 * <li>controlWhitelisted - multiple values - the expected whitelisted accounts</li>
 * <li>controlMaxFees - The maximum allowed accumulated total fees for not yet finished phased transactions.</li>
 * <li>controlMinDuration - The minimum phasing duration (finish height minus current height).</li>
 * <li>controlHolding - The maximum allowed phasing duration.</li>
 * </ul>
 *
 * 
 */
public final class SetPhasingOnlyControl extends CreateTransaction {

    private static class SetPhasingOnlyControlHolder {
        private static final SetPhasingOnlyControl INSTANCE = new SetPhasingOnlyControl();
    }

    public static SetPhasingOnlyControl getInstance() {
        return SetPhasingOnlyControlHolder.INSTANCE;
    }

    private SetPhasingOnlyControl() {
        super(new APITag[] {APITag.ACCOUNT_CONTROL, APITag.CREATE_TRANSACTION}, "controlVotingModel", "controlQuorum", "controlMinBalance",
                "controlMinBalanceModel", "controlHolding", "controlWhitelisted", "controlWhitelisted", "controlWhitelisted",
                "controlMaxFees", "controlMinDuration", "controlMaxDuration");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        Account account = ParameterParser.getSenderAccount(request);
        PhasingParams phasingParams = parsePhasingParams(request, "control");
        long maxFees = ParameterParser.getLong(request, "controlMaxFees", 0, Constants.getMaxBalanceATM(), false);
        short minDuration = (short)ParameterParser.getInt(request, "controlMinDuration", 0, Constants.MAX_PHASING_DURATION - 1, false);
        short maxDuration = (short) ParameterParser.getInt(request, "controlMaxDuration", 0, Constants.MAX_PHASING_DURATION - 1, false);
        return createTransaction(request, account, new Attachment.SetPhasingOnly(phasingParams, maxFees, minDuration, maxDuration));
    }

}
