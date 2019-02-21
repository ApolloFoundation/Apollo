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

package com.apollocurrency.aplwallet.apl.core.addons;


import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;

public final class Demo implements AddOn {
    private static final Logger LOG = getLogger(Demo.class);

    private BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();

    @Override
    public void init() {
        blockchainProcessor.addListener(block -> LOG.info("Block " + block.getStringId()
                + " has been forged by account " + Convert2.rsAccount(block.getGeneratorId()) + " having effective balance of "
                + Account.getAccount(block.getGeneratorId()).getEffectiveBalanceAPL()),
                BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
    }

    @Override
    public void shutdown() {
        LOG.info("Goodbye!");
    }

    @Override
    public void processRequest(Map<String, String> params) {
        LOG.info(params.get("demoMessage"));
    }
}
