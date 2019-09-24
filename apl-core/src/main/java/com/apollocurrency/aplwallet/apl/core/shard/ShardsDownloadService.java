/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.peer.FileDownloader;
import com.apollocurrency.aplwallet.apl.core.peer.ShardDownloader;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for background downloading of shard files and related files
 * @author alukin@gmail.com
 */
@Singleton
public class ShardsDownloadService {
    private final ShardDownloader shardDownloader;
    private final FileDownloader fileDownloader;
    
    @Inject
    public ShardsDownloadService(ShardDownloader shardDownloader, FileDownloader fileDownloader) {
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
}
