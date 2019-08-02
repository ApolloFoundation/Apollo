/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author al
 */
class PeerUnBlacklistingThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PeerUnBlacklistingThread.class);
    private final TimeService timeService;

    public PeerUnBlacklistingThread(TimeService timeService) {
        this.timeService = timeService;
    }

    @Override
    public void run() {
        try {
            try {
                int curTime = timeService.getEpochTime();
                for (Peer peer : Peers.getAllPeers()) {
                    ((PeerImpl)peer).updateBlacklistedStatus(curTime);
                }
            } catch (Exception e) {
                LOG.debug("Error un-blacklisting peer", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            System.exit(1);
        }
    }
    
}
