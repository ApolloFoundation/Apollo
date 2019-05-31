/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import javax.enterprise.inject.spi.CDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alukin@gmail.com
 */
class PeerLoaderThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PeerLoaderThread.class);
    private final List<String> defaultPeers;
    private EpochTime timeService;
    private final List<Future<String>> unresolvedPeers;

    public PeerLoaderThread(List<String> defaultPeers, List<Future<String>> unresolvedPeers, EpochTime timeService) {
        this.defaultPeers = defaultPeers;
        this.unresolvedPeers = unresolvedPeers;
        this.timeService=timeService;
    }
    private final Set<PeerDb.Entry> entries = new HashSet<>();
    private PeerDb peerDb;

    @Override
    public void run() {
        LOG.trace("'Peer loader': thread starting...");
        if (peerDb == null) {
            peerDb = CDI.current().select(PeerDb.class).get();
        }
        final int now = timeService.getEpochTime();
        Peers.wellKnownPeers.forEach((address) -> entries.add(new PeerDb.Entry(address, 0, now)));
        if (Peers.usePeersDb) {
            LOG.debug("'Peer loader': Loading 'well known' peers from the database...");
            defaultPeers.forEach((address) -> entries.add(new PeerDb.Entry(address, 0, now)));
            if (Peers.savePeers) {
                List<PeerDb.Entry> dbPeers = peerDb.loadPeers();
                dbPeers.forEach((entry) -> {
                    if (!entries.add(entry)) {
                        // Database entries override entries from chains.json
                        entries.remove(entry);
                        entries.add(entry);
                        LOG.trace("'Peer loader': Peer Loaded from db = {}", entry);
                    }
                });
            }
        }
        if (entries.size() > 0) {
            LOG.debug("'Peer loader': findOrCreatePeer() 'known peers'...");
        }
        entries.forEach((entry) -> {
            Future<String> unresolvedAddress = Peers.peersExecutorService.submit(() -> {
                PeerImpl peer = Peers.findOrCreatePeer(entry.getAddress(), true);
                if (peer != null) {
                    peer.setLastUpdated(entry.getLastUpdated());
                    peer.setServices(entry.getServices());
                    Peers.addPeer(peer);
                    LOG.trace("'Peer loader': Put 'well known' Peer from db into 'Peers Map' = {}", entry);
                    return null;
                }
                return entry.getAddress();
            });
            unresolvedPeers.add(unresolvedAddress);
        });
        LOG.trace("'Peer loader': thread finished. Peers [{}] =\n{}", Peers.getAllPeers().size());
        Peers.getAllPeers().stream().forEach((peerHost) -> LOG.trace("'Peer loader': dump = {}", peerHost));
    }
    
}
