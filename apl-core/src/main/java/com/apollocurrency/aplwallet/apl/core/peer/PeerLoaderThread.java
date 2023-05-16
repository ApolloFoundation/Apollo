/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author alukin@gmail.com
 */
@Slf4j
class PeerLoaderThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PeerLoaderThread.class);
    private final List<String> defaultPeers;
    private final List<Future<String>> unresolvedPeers;
    private final Set<PeerDb.Entry> entries = new HashSet<>();
    private final TimeService timeService;
    private final PeersService peersService;
    private final PeerDb peerDb;

    public PeerLoaderThread(List<String> defaultPeers,
                            List<Future<String>> unresolvedPeers,
                            TimeService timeService,
                            PeersService peersService) {
        this.defaultPeers = defaultPeers;
        this.unresolvedPeers = unresolvedPeers;
        this.timeService = timeService;
        this.peersService = peersService;
        this.peerDb = peersService.getPeerDb();
    }

    @Override
    public void run() {
        LOG.trace("'Peer loader': thread starting...");
        if (this.peerDb == null) {
//            peerDb = CDI.current().select(PeerDb.class).get();
            String error = "ERROR, the peerDb instance was not initialized inside peerService";
            log.error(error);
            throw new RuntimeException(error);
        }
        final int now = timeService.getEpochTime();
        peersService.wellKnownPeers.forEach((address) -> {
            PeerAddress pa = new PeerAddress(address);
            entries.add(new PeerDb.Entry(pa.getAddrWithPort(), 0, now));
        });
        if (PeersService.usePeersDb) {
            LOG.debug("'Peer loader': Loading 'well known' peers from the database...");
            defaultPeers.forEach((address) -> {
                PeerAddress pa = new PeerAddress(address);
                entries.add(new PeerDb.Entry(pa.getAddrWithPort(), 0, now));
            });
            if (PeersService.savePeers) {
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
        if (!entries.isEmpty()) {
            LOG.debug("'Peer loader': findOrCreatePeer() 'known peers'...");
        }
        entries.forEach((entry) -> {
            Future<String> unresolvedAddress = peersService.peersExecutorService.submit(() -> {
                Peer peer = peersService.findOrCreatePeer(null, entry.getAddress(), true);
                if (peer != null) {
                    peer.setLastUpdated(entry.getLastUpdated());
                    peer.setServices(entry.getServices());
                    peersService.addPeer(peer);
                    LOG.trace("'Peer loader': Put 'well known' Peer from db into 'Peers Map' = {}", entry);
                    return null;
                }
                return entry.getAddress();
            });
            unresolvedPeers.add(unresolvedAddress);
        });
        LOG.trace("'Peer loader': thread finished. Peers [{}]", entries.size());
        peersService.getAllPeers().stream().forEach((peerHost) -> LOG.trace("'Peer loader': dump = {}", peerHost));
    }

}
