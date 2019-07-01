/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkState;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.HasHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerFileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

/**
 * This class performs complete file downloading from peers
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileDownloader {

    @Vetoed
    public static class Status {
        double completed = 0.0;
        int chunksTotal = 1; //init to 1 to avoid zero division
        int chunksReady = 0;
        List<String> peers = new ArrayList<>();
        FileDownloadDecision decision = FileDownloadDecision.NotReady;
        boolean isComplete(){
            return chunksReady==chunksTotal;
        }
    }

    public static final int DOWNLOAD_THREADS = 6;
    private String fileID;
    private FileDownloadInfo downloadInfo;
    private List<HasHashSum> goodPeers;
    private List<HasHashSum> badPeers;
    private final Status status = new Status();

    private DownloadableFilesManager manager;
    private AplAppStatus aplAppStatus;
    private String taskId;

    ExecutorService executor;
    List<Future<Boolean>> runningDownloaders = new ArrayList<>();
    private javax.enterprise.event.Event<ShardPresentData> presentDataEvent;

    @Inject
    public FileDownloader(DownloadableFilesManager manager,
                          javax.enterprise.event.Event<ShardPresentData> presentDataEvent,
                          AplAppStatus aplAppStatus) {
        this.manager = Objects.requireNonNull(manager, "manager is NULL");
        this.executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        this.presentDataEvent = Objects.requireNonNull(presentDataEvent, "presentDataEvent is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
    }
    
    public void setFileId(String fileID){
      if(this.fileID==null){
        this.fileID=fileID;
      }else{
          throw new RuntimeException("Can not set new filed ID in FileDownloader, it is already set");
      }
    }
    
    public void startDownload() {
        this.taskId = this.aplAppStatus.durableTaskStart("FileDownload", "Downloading file from Peers...", true);
        log.debug("startDownload()...");
        CompletableFuture<Boolean> prepare;
        prepare = CompletableFuture.supplyAsync(() -> {
            status.decision = prepareForDownloading(null);
            Boolean res = (status.decision == FileDownloadDecision.AbsOK || status.decision == FileDownloadDecision.OK);
            return res;
        });
        
        prepare.thenAccept( r->{
            if (r) {
                status.chunksTotal = downloadInfo.chunks.size();
                log.debug("Decision is OK: {}, starting chunks downloading", status.decision.name());
                download();
            } else {
                log.warn("Decision is not OK: {}, Chunks downloading is not started",status.decision.name());
            }                
        });
    }

    public Status getDownloadStatus() {
        status.completed = ((1.0D * status.chunksReady) / (1.0 * status.chunksTotal)) * 100.0;
        return status;
    }

    public FileDownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public FileDownloadDecision prepareForDownloading(Set<Peer> onlyPeers) {
        log.debug("prepareForDownloading()...");
        FileDownloadDecision res;
        Set<Peer> allPeers;
        if(onlyPeers==null || onlyPeers.isEmpty()){
           allPeers = getAllAvailablePeers();
        }else{
           allPeers=new HashSet<>(); 
           allPeers.addAll(onlyPeers);
        }
        log.debug("prepareForDownloading(), allPeers = {}", allPeers);
        PeersList pl = new PeersList();
        allPeers.forEach((pi) -> {
            PeerFileInfo pfi = new PeerFileInfo(new PeerClient(pi), fileID);
            pl.add(pfi);
        });
        log.debug("prepareForDownloading(), pl = {}", pl);
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeers, badPeers);
        if(pvdm.isNetworkUsable()){
            PeerFileInfo pfi = (PeerFileInfo)goodPeers.get(0);
            downloadInfo = pfi.getFdi();
        }
        log.debug("prepareForDownloading(), res = {}", res);
        return res;
    }

    private synchronized FileChunkInfo getNextEmptyChunk() {
        log.debug("getNextEmptyChunk()...");
        FileChunkInfo res = null;
        for (FileChunkInfo fci : downloadInfo.chunks) {
            if (fci.present.ordinal() < FileChunkState.DOWNLOAD_IN_PROGRESS.ordinal()) {
                res = fci;
                log.debug("getNextEmptyChunk() fci.present < FileChunkState.DOWNLOAD_IN_PROGRESS...{}", fci.present.ordinal());
                break;
            }
            this.aplAppStatus.durableTaskUpdate(this.taskId,
                    (double) (downloadInfo.chunks.size() / Math.max (fci.chunkId, 1) ), "File downloading...");
        }
        if (res == null) { //NO more empty chunks. File is ready
            log.debug("getNextEmptyChunk() fileID = {}", fileID);
            this.aplAppStatus.durableTaskFinished(this.taskId, false, "File downloading finished");
            //FIRE event when shard is PRESENT + ZIP is downloaded
            ShardPresentData shardPresentData = new ShardPresentData(fileID);
            presentDataEvent.select(literal(ShardPresentEventType.PRESENT)).fireAsync(shardPresentData);
        }
        return res;
    }

    private boolean doPeerDownload(PeerClient p) throws IOException {
        boolean res = true;
        FileChunkInfo fci = getNextEmptyChunk();
        ChunkedFileOps fops = new ChunkedFileOps(manager.mapFileIdToLocalPath(fileID));
        while (fci != null) {
            fci.present=FileChunkState.DOWNLOAD_IN_PROGRESS;
            FileChunk fc = p.downloadChunk(fci);
            if(fc!=null){
                byte[] data = Base64.getDecoder().decode(fc.mime64data);
                fops.writeChunk(fc.info.offset, data, fc.info.crc);
                status.chunksReady++;
                fci.present = FileChunkState.SAVED;
            }else{
//              fci.present=FileChunkState.PRESENT;
              fci.present=FileChunkState.NOT_PRESENT; // no info, no connect ??
            }
            fci = getNextEmptyChunk();
        }
        log.debug("doPeerDownload() fci.present < FileChunkState.DOWNLOAD_IN_PROGRESS...{}", fci.present.ordinal());
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
    
    @PreDestroy
    public void preDestroy(){
        if(executor!=null){
            //TODO: do we need to cancel tasks and threads?
            executor.shutdown();
        }
    }

    private AnnotationLiteral<ShardPresentEvent> literal(ShardPresentEventType shardPresentEventType) {
        return new ShardPresentEventBinding() {
            @Override
            public ShardPresentEventType value() {
                return shardPresentEventType;
            }
        };
    }

}
