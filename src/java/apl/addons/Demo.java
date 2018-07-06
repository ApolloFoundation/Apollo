/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.addons;

import apl.Account;
import apl.BlockchainProcessor;
import apl.Apl;
import apl.util.Convert;
import apl.util.Logger;

import java.util.Map;

public final class Demo implements AddOn {

    @Override
    public void init() {
        Apl.getBlockchainProcessor().addListener(block -> Logger.logInfoMessage("Block " + block.getStringId()
                + " has been forged by account " + Convert.rsAccount(block.getGeneratorId()) + " having effective balance of "
                + Account.getAccount(block.getGeneratorId()).getEffectiveBalanceAPL()),
                BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
    }

    @Override
    public void shutdown() {
        Logger.logInfoMessage("Goodbye!");
    }

    @Override
    public void processRequest(Map<String, String> params) {
        Logger.logInfoMessage(params.get("demoMessage"));
    }
}
