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

package com.apollocurrency.aplwallet.apl.peer;

import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.Apl;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty extends PeerServlet.PeerRequestHandler {

    private static class GetCumulativeDifficultyHolder {
        private static final GetCumulativeDifficulty INSTANCE = new GetCumulativeDifficulty();
    }

    public static GetCumulativeDifficulty getInstance() {
        return GetCumulativeDifficultyHolder.INSTANCE;
    }

    private GetCumulativeDifficulty() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        Block lastBlock = Apl.getBlockchain().getLastBlock();
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("blockchainHeight", lastBlock.getHeight());
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
