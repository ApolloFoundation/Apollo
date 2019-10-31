/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadedEvent;
import com.apollocurrency.aplwallet.apl.core.files.FileEventData;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for background downloading of shard files and related files
 * @author alukin@gmail.com
 */
@Singleton
@Slf4j
public class ShardsDownloadService {
    private final ShardInfoDownloader shardInfoDownloader;
    private final UUID myChainId;
    
    private final javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    private final FileDownloadService fileDownloadService;
    private final DownloadableFilesManager downloadableFilesManager;
    private final PropertiesHolder propertiesHolder;
    private final ShardNameHelper shardNameHelper = new ShardNameHelper();   
    //shardId:fileId
    private final Map<Long,String> shardDownloaded = new HashMap<>();
    @Inject @Any
    Event<ShardPresentData> shardDataEvent;
     
    @Inject
    public ShardsDownloadService(ShardInfoDownloader shardInfoDownloader, 
            BlockchainConfig blockchainConfig,
            javax.enterprise.event.Event<ShardPresentData> presentDataEvent,
            PropertiesHolder propertiesHolder,
            DownloadableFilesManager downloadableFilesManager,
            FileDownloadService fileDownloadService
            ) 
    {
        this.shardInfoDownloader = shardInfoDownloader;
        this.fileDownloadService = fileDownloadService;
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.downloadableFilesManager = downloadableFilesManager;
        this.presentDataEvent = presentDataEvent;
        this.propertiesHolder = propertiesHolder;
    }
    
    public boolean getShardingInfoFromPeers(){
       boolean res = false;
       shardInfoDownloader.getShardInfoFromPeers();
       return res;
    }
    
    private static boolean isAcceptable(FileDownloadDecision d) {
        boolean res = (d == FileDownloadDecision.AbsOK || d == FileDownloadDecision.OK );
        return res;
    } 
    
    private AnnotationLiteral<ShardPresentEvent> literal(ShardPresentEventType shardPresentEventType) {
        return new ShardPresentEventBinding() {
            @Override
            public ShardPresentEventType value() {
                return shardPresentEventType;
            }
        };
    }  

    public void onAnyFileDownloadEevent(@Observes @FileDownloadedEvent FileEventData fileData){
        //TODO: process events carefully
        Long shardId = 0L;
        fireShardPresentEvent(shardId);
    }
    
    private void fireNoShardEvent() {
        ShardPresentData shardPresentData = new ShardPresentData();
        log.debug("Firing 'NO_SHARD' event...");
        presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fire(shardPresentData); // data is ignored
    }
    
    private void fireShardPresentEvent(Long shardId) {
        ShardNameHelper snh = new ShardNameHelper();
        String fileId = snh.getFullShardId(shardId, myChainId);
        ShardPresentData shardPresentData = new ShardPresentData(fileId);
        log.debug("Firing 'SHARD_PRESENT' event {}...", shardPresentData);
        presentDataEvent.select(literal(ShardPresentEventType.SHARD_PRESENT)).fireAsync(shardPresentData); // data is used
    }
    
    private boolean checkShardDownloadedAlready(Long shardId, byte[] hash) {
        boolean res = false;
        // check if zip file exists on local node
        String shardFileId = shardNameHelper.getFullShardId(shardId, myChainId);
        File zipInExportedFolder = downloadableFilesManager.mapFileIdToLocalPath(shardFileId).toFile();
        log.debug("Checking existence zip = '{}', ? = {}", zipInExportedFolder, zipInExportedFolder.exists());
        if (zipInExportedFolder.exists()) {
            log.info("No need to download '{}'  as it is found in path = '{}'", shardFileId, zipInExportedFolder.toString());
            //check integrity
            FileInfo fi = downloadableFilesManager.getFileInfo(shardFileId);
            String fileHashActual = fi.hash;
            String receivedHash = Convert.toHexString(hash);
            if (fileHashActual.equalsIgnoreCase(receivedHash)) {
                res = true;
                log.debug("Good zip hash was computed return '{}'...", res);
            } else {
                boolean deleteResult = zipInExportedFolder.delete();
                res = false;
                log.debug("bad shard file: '{}', received hash: '{}'. Calculated hash: '{}'. Zip is deleted = '{}'",
                        zipInExportedFolder.getAbsolutePath(), receivedHash, fileHashActual, deleteResult);
            }
        }
        return res;
    }
    
   private List<String> checkAdditionalFiles(Long shardId){
       List<String> additionalFilesIDs = new ArrayList<>();
       ShardInfo si = shardInfoDownloader.getShardInfo(shardId);
       return additionalFilesIDs;
   }
   
   public FileDownloadDecision tryDownloadShard(Long shardId) {
        FileDownloadDecision result;
        log.debug("Processing shardId '{}'", shardId);
        result = shardInfoDownloader.getShardsDesisons().get(shardId);
        if (!isAcceptable(result)) {
            log.warn("Shard {} can not be loaded from peers", shardId);
            return result;
        }
        

        // check if zip file exists on local node
        PeerFileHashSum good = shardInfoDownloader.getGoodPeersMap().get(shardId).iterator().next();
        byte[] goodHash = good.getHash();
        if (checkShardDownloadedAlready(shardId, goodHash)) {
            result = FileDownloadDecision.OK;
            return result;
        }
        log.debug("Start preparation to downloading...");
        String fileID = shardNameHelper.getFullShardId(shardId, myChainId);
        log.debug("fileID = '{}'", fileID);
        
        if (isAcceptable(result)) {            
            log.debug("Starting shard downloading: '{}'", fileID);
            Set<String> peers = new HashSet<>();
            shardInfoDownloader.getGoodPeersMap().get(shardId).forEach((pfhs) -> {
                peers.add(pfhs.getPeerId());
            });
            fileDownloadService.startDownload(fileID, peers);
            checkAdditionalFiles(shardId);
        } else {
            log.warn("Can not find enough peers with good shard: '{}' because result '{}'", fileID, result);
            fireNoShardEvent();
            // We CAN'T download latest SHARD archive, start from the beginning - FIRE event here
        }

        return result;
    }
     public FileDownloadDecision prepareAndStartDownload() {
        boolean goodShardFound = false;
        log.debug("prepareAndStartDownload...");
        boolean doNotShardImport = propertiesHolder.getBooleanProperty("apl.noshardimport", false);
        FileDownloadDecision result = FileDownloadDecision.NotReady;
        if(doNotShardImport){
            fireNoShardEvent();
            result=FileDownloadDecision.NoPeers;
            log.warn("prepareAndStartDownload: skipping shard import due to config/command-line option");
            return result;        
        }

        if (shardInfoDownloader.getSortedShards().isEmpty()) {
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("result = {}, Fire = {}", result, "NO_SHARD");
            fireNoShardEvent();
            return result;
        } else {
            //we have some shards available on the networks, let's decide what to do
            List<Long> shardIds = new ArrayList(shardInfoDownloader.getSortedShards().keySet());
            Collections.sort(shardIds, Collections.reverseOrder());
            for (Long shardId : shardIds) {
                result = tryDownloadShard(shardId);
                goodShardFound = isAcceptable(result);
                if (goodShardFound) {
                    break;
                }
            }
            if (!goodShardFound) {
                fireNoShardEvent();
            }
        }
        return result;
    }
   
}
