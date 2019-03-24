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

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

@Vetoed
final class GetCumulativeDifficulty extends PeerRequestHandler {

    public GetCumulativeDifficulty() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        Block lastBlock = lookupBlockchain().getLastBlock();
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("blockchainHeight", lastBlock.getHeight());
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
