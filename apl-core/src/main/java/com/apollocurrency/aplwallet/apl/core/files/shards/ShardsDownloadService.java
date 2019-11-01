/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.FileEventData;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
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
import java.util.Map;
import java.util.HashMap;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadEvent;

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
    private final PropertiesHolder propertiesHolder;
    private final ShardNameHelper shardNameHelper = new ShardNameHelper();   
    private final Map<Long,ShardDownloadStatus> shardDownloadStatuses = new HashMap<>();
    @Inject @Any
    Event<ShardPresentData> shardDataEvent;
     
    @Inject
    public ShardsDownloadService(ShardInfoDownloader shardInfoDownloader, 
            BlockchainConfig blockchainConfig,
            javax.enterprise.event.Event<ShardPresentData> presentDataEvent,
            PropertiesHolder propertiesHolder,
            FileDownloadService fileDownloadService
            ) 
    {
        this.shardInfoDownloader = shardInfoDownloader;
        this.fileDownloadService = fileDownloadService;
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.presentDataEvent = presentDataEvent;
        this.propertiesHolder = propertiesHolder;
    }
    
    public boolean getShardingInfoFromPeers(){
       Map<Long,Set<ShardInfo>> shards = shardInfoDownloader.getShardInfoFromPeers();
       shards.keySet().forEach((sId) -> {
           ShardInfo si =  shardInfoDownloader.getShardInfo(sId);
           List<String> shardFiles = new ArrayList<>();
           shardFiles.add(shardNameHelper.getFullShardId(sId, myChainId));
           shardFiles.addAll(si.additionalFiles);
           ShardDownloadStatus st = new ShardDownloadStatus(shardFiles);
           shardDownloadStatuses.put(sId, st);
        });    
       return !shards.isEmpty();
    }

    public void onAnyFileDownloadEevent(@Observes @FileDownloadEvent FileEventData fileData){
        //TODO: process events carefully
        Long shardId = 0L;
        fireShardPresentEvent(shardId);
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

    private void fireNoShardEvent() {
        ShardPresentData shardPresentData = new ShardPresentData();
        log.debug("Firing 'NO_SHARD' event...");
        presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fire(shardPresentData); // data is ignored
    }
    
    private void fireShardPresentEvent(Long shardId) {
        ShardNameHelper snh = new ShardNameHelper();
        String fileId = snh.getFullShardId(shardId, myChainId);
        ShardInfo si = shardInfoDownloader.getShardInfo(shardId);
        ShardPresentData shardPresentData = new ShardPresentData(
                shardId,
                fileId,
                si.additionalFiles
        );
        log.debug("Firing 'SHARD_PRESENT' event {}...", shardPresentData);
        presentDataEvent.select(literal(ShardPresentEventType.SHARD_PRESENT)).fireAsync(shardPresentData); // data is used
    }
    
    /**
     * Checks if files from shard are available locally and OK
     * @param si shard info record
     * @return list of files we yet have to download from peers
     */
    private List<String> checkShardDownloadedAlready(ShardInfo si) {
        List<String> res = new ArrayList<>();
        // check if zip file exists on local node
        String shardFileId = shardNameHelper.getFullShardId(si.shardId, myChainId);
        if(!fileDownloadService.isFileDownloadedAlready(shardFileId,si.zipCrcHash)){
            res.add(shardFileId);
        }
        for(int i=0; i<si.additionalFiles.size();i++){
            if(!fileDownloadService.isFileDownloadedAlready(si.additionalFiles.get(i), si.additionalHashes.get(i))){
                res.add(si.additionalFiles.get(i));
            }
        }
        return res;
    }
   
   public FileDownloadDecision tryDownloadShard(Long shardId) {
        FileDownloadDecision result;
        log.debug("Processing shardId '{}'", shardId);
        result = shardInfoDownloader.getShardsDesisons().get(shardId);
        if (!isAcceptable(result)) {
            log.warn("Shard {} can not be loaded from peers", shardId);
            return result;
        }

        // check if shard files exist on local node
        ShardInfo si = shardInfoDownloader.getShardInfo(shardId);
        List<String> filesToDownload = checkShardDownloadedAlready(si);
        if (filesToDownload.isEmpty()) {
            result = FileDownloadDecision.OK;
            return result;
        }
        log.debug("Start downloading missing shard files...");
       Set<String> peers = new HashSet<>();
       shardInfoDownloader.getGoodPeersMap().get(shardId).forEach((pfhs) -> {
           peers.add(pfhs.getPeerId());
       });       
       for(String fileId: filesToDownload){
           fileDownloadService.startDownload(fileId, peers);
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
