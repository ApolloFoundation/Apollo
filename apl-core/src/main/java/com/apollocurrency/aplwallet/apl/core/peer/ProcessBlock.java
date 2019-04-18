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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

@Vetoed
final class ProcessBlock extends PeerRequestHandler {
    private static final Logger LOG = getLogger(ProcessBlock.class);

    public ProcessBlock() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        String previousBlockId = (String)request.get("previousBlock");
        Block lastBlock = lookupBlockchain().getLastBlock();
        long peerBlockTimestamp = Convert.parseLong(request.get("timestamp"));
        Object timeoutJsonValue = request.get("timeout");
        int peerBlockTimeout =  timeoutJsonValue == null ? 0 : ((Long)timeoutJsonValue).intValue();
        if (lastBlock.getStringId().equals(previousBlockId) ||
                (Convert.parseUnsignedLong(previousBlockId) == lastBlock.getPreviousBlockId()
                        && (lastBlock.getTimestamp() > peerBlockTimestamp ||
                        peerBlockTimestamp == lastBlock.getTimestamp() && peerBlockTimeout > lastBlock.getTimeout()))) {
            Peers.peersService.submit(() -> {
                try {
                    LOG.debug("API: need to process better peer block");
                    lookupBlockchainProcessor().processPeerBlock(request);
                } catch (AplException | RuntimeException e) {
                    if (peer != null) {
                        peer.blacklist(e);
                    }
                }
            });
        }
        return JSON.emptyJSON;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
