/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunkState;
import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.HasHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerFileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

/**
 * This class performs complete file downloading from peers
 *
 * @author alukin@gmail.com
 */
public class FileDownloader {

    @Vetoed
    public class Status {

        double completed;
        int chunksTotal;
        int chunksReady;
        List<String> peers;
    }
    public static final int DOWNLOAD_THREADS = 4;
    private String fileID;
    private FileDownloadInfo downloadInfo;
    private List<HasHashSum> goodPeers;
    private List<HasHashSum> badPeers;
    private Status status;

    @Inject
    DownloadableFilesManager manager;
    ExecutorService executor;
    List<Future<Boolean>> runningDownloaders = new ArrayList<>();
    
    public FileDownloader() {
        this.executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
    }

    public void startDownload(String fileID) {
        this.fileID = fileID;
    }

    public Status getDownloadStatus() {
        status.completed = 1.0D * status.chunksTotal / status.chunksReady;
        return status;
    }

    public FileDownloadDecision prepareForDownloading() {
        FileDownloadDecision res;
        Set<Peer> allPeers = getAllAvailablePeers();
        PeersList pl = new PeersList();
        allPeers.forEach((pi) -> {
            PeerFileInfo pfi = new PeerFileInfo(new PeerClient(pi), fileID);
            pl.add(pfi);
        });
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();
        return res;
    }

    private synchronized FileChunkInfo getNextEmptyChunk() {
        FileChunkInfo res = null;
        for (FileChunkInfo fci : downloadInfo.chunks) {
            if (fci.present.ordinal() < FileChunkState.DOWNLOAD_IN_PROCGRESS.ordinal()) {
                res = fci;
                break;
            }
        }
        return res;
    }


    private boolean doPeerDownload(PeerClient p) throws IOException {
        boolean res = true;
        FileChunkInfo fci = getNextEmptyChunk();
        ChunkedFileOps fops = new ChunkedFileOps(manager.mapFileIdToLocalPath(fileID));
        while (fci != null) {
            FileChunk fc = p.downloadChunk(fci);
            byte[] data = new byte[fc.info.size];
            fops.writeChunk(fc.info.offset, data, fc.info.crc);
            status.chunksReady++;
            fci.present = FileChunkState.SAVED;
            fci = getNextEmptyChunk();
        }
        return res;
    }

    public Status download() {
        int peerCount = 0;
        for (HasHashSum p : goodPeers) {
            Future<Boolean> dn_res = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    PeerFileInfo pfi = (PeerFileInfo) p;
                    return doPeerDownload(pfi.getPeerClient());
                }
            });
            runningDownloaders.add(dn_res);
            peerCount++;
            if (peerCount > DOWNLOAD_THREADS) {
                break;
            }
        }
        return status;
    }

    public Set<Peer> getAllAvailablePeers() {
        Set<Peer> res = new HashSet<>();
        Collection<? extends Peer> knownPeers = Peers.getAllPeers();
        res.addAll(knownPeers);
        return res;
    }
}
