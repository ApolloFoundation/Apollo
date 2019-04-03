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

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class PopOffCounter implements AddOn {
    private static final Logger LOG = getLogger(PopOffCounter.class);

    private volatile int numberOfPopOffs = 0;
    private BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();

    @Override
    public void init() {
        blockchainProcessor.addListener(block -> numberOfPopOffs += 1, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    @Override
    public AbstractAPIRequestHandler getAPIRequestHandler() {
        return new AbstractAPIRequestHandler(new APITag[]{APITag.ADDONS, APITag.BLOCKS}) {
            @Override
            public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
                JSONObject response = new JSONObject();
                response.put("numberOfPopOffs", numberOfPopOffs);
                return response;
            }
            @Override
            protected boolean allowRequiredBlockParameters() {
                return false;
            }
        };
    }

    @Override
    public String getAPIRequestType() {
        return "getNumberOfPopOffs";
    }

    @Override
    public void processRequest(Map<String, String> params) {
        LOG.info(params.get("popOffMessage"));
    }
}
