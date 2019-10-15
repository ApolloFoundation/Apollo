/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadedEvent;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloader;
import com.apollocurrency.aplwallet.apl.core.files.FileEventData;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for background downloading of shard files and related files
 * @author alukin@gmail.com
 */
@Singleton
public class ShardsDownloadService {
    private final ShardInfoDownloader shardDownloader;
    private final FileDownloader fileDownloader;
    @Inject @Any
    Event<ShardPresentData> shardDataEvent;
     
    @Inject
    public ShardsDownloadService(ShardInfoDownloader shardDownloader, FileDownloader fileDownloader) {
        this.shardDownloader = shardDownloader;
        this.fileDownloader = fileDownloader;
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
}
