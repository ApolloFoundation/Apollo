/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkState;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.PeerAddress;
import java.util.HashSet;
import javax.enterprise.event.Event;

/**
 * This class performs complete file downloading from peers
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileDownloader {


    public static final int DOWNLOAD_THREADS = 6;
    private String fileID;
    private FileDownloadInfo downloadInfo;

    private final DownloadableFilesManager manager;
    private final AplAppStatus aplAppStatus;
    private String taskId;
    private final ReadWriteLock fileChunksLock =  new ReentrantReadWriteLock();
    private final AtomicLong lastPercent = new AtomicLong(0L);
            
    ExecutorService executor;
    List<Future<Boolean>> runningDownloaders = new ArrayList<>();
    private final Event<FileEventData> fileEvent;
    @Getter
    private CompletableFuture<Boolean> downloadTask;
    @Getter
    private FileDownloadStatus status;
    private final Set<Peer> peers=new HashSet<>();
    private final PeersService peersService;
    @Inject
    public FileDownloader(DownloadableFilesManager manager,
                          Event<FileEventData> fileEvent,
                          AplAppStatus aplAppStatus,
                          PeersService peers) {
        this.manager = Objects.requireNonNull(manager, "manager is NULL");
        this.executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        this.fileEvent = fileEvent;
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        this.peersService=peers;
    }
    
    public void startDownload(FileDownloadInfo downloadInfo, FileDownloadStatus status, Set<PeerFileHashSum> goodPeers) {
        this.downloadInfo = downloadInfo;
        this.status=status;
        peers.clear();
        for(PeerFileHashSum ps: goodPeers){
           PeerAddress actualAddr = new PeerAddress(ps.getPeerId());
           peers.add((Peer)peersService.findOrCreatePeer(actualAddr, null, false));
        }
        fileID = downloadInfo.fileInfo.fileId;
        this.taskId = this.aplAppStatus.durableTaskStart("FileDownload", "Downloading file from Peers...", true);
        log.debug("startDownload()...");
        downloadTask = CompletableFuture.supplyAsync(() -> {
                status.chunksTotal.set(downloadInfo.chunks.size());
                status.chunksReady.set(0);
                updateStatus();
                log.debug("Starting file chunks downloading");
                download();
                return status.isComplete();
        });
    }

    private void updateStatus() {
        status.completed = ((1.0D * status.chunksReady.get()) / (1.0D * status.chunksTotal.get())) * 100.0D;
    }

    private FileChunkInfo getNextEmptyChunk() {
        FileChunkInfo res = null;
        fileChunksLock.writeLock().lock();
        try {
            for (FileChunkInfo fci : downloadInfo.chunks) {
                if (fci.present.ordinal() < FileChunkState.DOWNLOAD_IN_PROGRESS.ordinal()) {
                    res = fci;
                    fci.present=FileChunkState.DOWNLOAD_IN_PROGRESS;
                    log.trace("getNextEmptyChunk(): state: {}", fci.present);
                    break;
                }
            }
            updateStatus();
        } finally {
            fileChunksLock.writeLock().unlock();
        }
        return res;
    }
    //TODO: change to more general signal, not shard
    private void signalFinishedOK() {
        log.debug("signaling finished fileID = {}", fileID);
        this.aplAppStatus.durableTaskFinished(this.taskId, false, "File downloading finished: " + fileID);
        //FIRE event when shard is PRESENT + ZIP is downloaded
        FileEventData data = new FileEventData(
                fileID,
                true,
                ""
        );
        log.debug("Firing 'FILE_DOWNLOADED_PRESENT' event {}", data);
        fileEvent.fireAsync(data);
    }
    //TODO: change to more general signal, not shard   

    private void signalFailed(String reason) {
          FileEventData data = new FileEventData(
                fileID,
                false,
                reason
        );      
        fileEvent.fireAsync(data);
    } 
      
    private void setFileChunkState(FileChunkState state, FileChunkInfo fci){
        fileChunksLock.writeLock().lock();
        try {
            fci.present=state;
        } finally {
            fileChunksLock.writeLock().unlock();
        }
    }
    
    private boolean downloadAndSaveChunk(FileChunkInfo fci, PeerClient p, ChunkedFileOps fops) {
        boolean isLast=false;
        FileChunk fc = p.downloadChunk(fci);
        if (fc != null) {
            byte[] data = Base64.getDecoder().decode(fc.mime64data);
            try {
                fops.writeChunk(fc.info.offset, data, fc.info.crc);
                setFileChunkState(FileChunkState.SAVED, fci);
                status.chunksReady.incrementAndGet();
                //is the very last chunk succeed?
                if(status.chunksReady.get()>=downloadInfo.chunks.size()-1){
                     isLast=true;
                }
            } catch (IOException ex) {
                log.debug("Failed to download or save chunk: {} \n exception: {}",fci.chunkId,ex);
                setFileChunkState(FileChunkState.PRESENT_IN_PEER, fci); // may be next time we'll get it right
            }
        } else {
            setFileChunkState(FileChunkState.PRESENT_IN_PEER, fci);  //well, it exists anyway on some peer
        }
        return isLast;
    }
    
    private boolean doPeerDownload(PeerClient p) throws IOException {
        boolean res = true;
        FileChunkInfo fci;
        ChunkedFileOps fops = new ChunkedFileOps(manager.mapFileIdToLocalPath(fileID));
        while ((fci = getNextEmptyChunk())!= null) {
            boolean isLast = downloadAndSaveChunk(fci, p, fops);
            if(fci.present==FileChunkState.SAVED){
                long percent = Math.round(status.completed);
                if(lastPercent.get()+5<percent){
                    lastPercent.set(percent);
                    aplAppStatus.durableTaskUpdate(this.taskId, status.completed, "File downloading: "+this.fileID+"...");            
                }
            }
            if(isLast){
                break;
            }
        }
        log.debug("doPeerDownload() for peer {} finished", p.gePeer().getAnnouncedAddress());
        return res;
    }

    public FileDownloadStatus download() {
        int peerCount = 0;
        for (Peer p : peers) {
            Future<Boolean> dn_res = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return doPeerDownload(new PeerClient(p));
                }
            });
            runningDownloaders.add(dn_res);
            peerCount++;
            if (peerCount > DOWNLOAD_THREADS) {
                break; 
            }
        }
        boolean allOk=true;
        for(Future<Boolean> dn_task: runningDownloaders){
            try {
                allOk= allOk && dn_task.get();
            } catch (InterruptedException ex) {
                allOk=false;
                log.error("Some subtask of file downloader has been interrupted");
            } catch (ExecutionException ex) {
                log.error("Some subtask of file downloader has failed");
                allOk=false;
            }
        }
        if(allOk){
            signalFinishedOK();
        }else{
            signalFailed("File downloading failed");
        }
        return status;
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
