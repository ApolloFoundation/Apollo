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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
@Vetoed
public final class PopOffCounter implements AddOn {
    private static final Logger LOG = getLogger(PopOffCounter.class);

    private AtomicInteger numberOfPopOffs = new AtomicInteger();

    @Override
    public void init() {
    }

    public void onBlockPushed(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        numberOfPopOffs.incrementAndGet();
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
