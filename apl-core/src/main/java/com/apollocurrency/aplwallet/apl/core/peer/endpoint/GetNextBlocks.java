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

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;

public final class GetNextBlocks extends PeerRequestHandler {

    static final JSONStreamAware TOO_MANY_BLOCKS_REQUESTED;
    static final JSONStreamAware NO_BLOCK_ID_LIST;

    static {
        JSONObject tooManyBlocksResponse = new JSONObject();
        tooManyBlocksResponse.put("error", Errors.TOO_MANY_BLOCKS_REQUESTED);
        TOO_MANY_BLOCKS_REQUESTED = JSON.prepare(tooManyBlocksResponse);

        JSONObject noBlockIdListResponse = new JSONObject();
        noBlockIdListResponse.put("error", Errors.NO_BLOCK_ID_LIST);
        NO_BLOCK_ID_LIST = JSON.prepare(noBlockIdListResponse);
    }

    public GetNextBlocks() {
    }


    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();
        List<? extends Block> blocks;
        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<String> stringList = (List<String>) request.get("blockIds");
        Blockchain blockchain = lookupBlockchain();
        if (stringList == null) {
            return NO_BLOCK_ID_LIST;
        }
        if (stringList.size() > 36) {
            return TOO_MANY_BLOCKS_REQUESTED;
        }
        List<Long> idList = new ArrayList<>();
        stringList.forEach(stringId -> idList.add(Convert.parseUnsignedLong(stringId)));
        blocks = blockchain.getBlocksAfter(blockId, idList);
        blocks.forEach(block -> nextBlocksArray.add(block.getJSONObject()));
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
