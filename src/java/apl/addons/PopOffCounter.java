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

package apl.addons;

import apl.BlockchainProcessor;
import apl.Apl;
import apl.AplException;
import apl.http.APIServlet;
import apl.http.APITag;
import apl.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public final class PopOffCounter implements AddOn {

    private volatile int numberOfPopOffs = 0;

    @Override
    public void init() {
        Apl.getBlockchainProcessor().addListener(block -> numberOfPopOffs += 1, BlockchainProcessor.Event.BLOCK_POPPED);
    }

    @Override
    public APIServlet.APIRequestHandler getAPIRequestHandler() {
        return new APIServlet.APIRequestHandler(new APITag[]{APITag.ADDONS, APITag.BLOCKS}) {
            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
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
        Logger.logInfoMessage(params.get("popOffMessage"));
    }
}
