/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadedEvent;
import com.apollocurrency.aplwallet.apl.core.files.FileEventData;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import java.util.UUID;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for background downloading of shard files and related files
 * @author alukin@gmail.com
 */
@Singleton
@Slf4j
public class ShardsDownloadService {
    private final ShardInfoDownloader shardInfoDownloader;
    private final FileDownloadService fileDownloadService;
    private final UUID myChainId;
    private final javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    
    @Inject @Any
    Event<ShardPresentData> shardDataEvent;
     
    @Inject
    public ShardsDownloadService(ShardInfoDownloader shardInfoDownloader, 
            BlockchainConfig blockchainConfig,
            javax.enterprise.event.Event<ShardPresentData> presentDataEvent,
            FileDownloadService fileDownloadService) {
        this.shardInfoDownloader = shardInfoDownloader;
        this.fileDownloadService = fileDownloadService;
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.presentDataEvent = presentDataEvent;
    }
    
    public boolean getShardingInfoFromPeers(){
       boolean res = false;
       return res;
    }
    public void startLastShardDownload(){
        
    }
    
    public void startShardDownload(int idx){
        
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
        log.debug("Firing 'SHARD_PRESENT' event {}...", fileId);
        presentDataEvent.select(literal(ShardPresentEventType.SHARD_PRESENT)).fire(shardPresentData); // data is used
    }    
}
