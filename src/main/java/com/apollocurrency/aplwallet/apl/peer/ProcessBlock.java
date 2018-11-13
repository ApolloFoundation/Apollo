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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

final class ProcessBlock extends PeerServlet.PeerRequestHandler {
    private static final Logger LOG = getLogger(ProcessBlock.class);

    private static class ProcessBlockHolder {
        private static final ProcessBlock INSTANCE = new ProcessBlock();
    }

    public static ProcessBlock getInstance() {
        return ProcessBlockHolder.INSTANCE;
    }

    private ProcessBlock() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        String previousBlockId = (String)request.get("previousBlock");
        Block lastBlock = Apl.getBlockchain().getLastBlock();
        long peerBlockTimestamp = Convert.parseLong(request.get("timestamp"));
        int peerBlockTimeout = ((Long)request.get("timeout")).intValue();
        LOG.debug("API: Timeout: peerBlock{},ourBlock{}", peerBlockTimeout, lastBlock.getTimeout());
        LOG.debug("API: Timestamp: peerBlock{},ourBlock{}", peerBlockTimestamp, lastBlock.getTimestamp());
        LOG.debug("API: PrevId: peerBlock{},ourBlock{}", Convert.parseUnsignedLong(previousBlockId), lastBlock.getPreviousBlockId());
        if (lastBlock.getStringId().equals(previousBlockId) ||
                (Convert.parseUnsignedLong(previousBlockId) == lastBlock.getPreviousBlockId()
                        && (lastBlock.getTimestamp() > peerBlockTimestamp ||
                        peerBlockTimestamp == lastBlock.getTimestamp() && peerBlockTimeout > lastBlock.getTimeout()))) {
            Peers.peersService.submit(() -> {
                try {
                    LOG.debug("API: need to process peer block");
                    Apl.getBlockchainProcessor().processPeerBlock(request);
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
