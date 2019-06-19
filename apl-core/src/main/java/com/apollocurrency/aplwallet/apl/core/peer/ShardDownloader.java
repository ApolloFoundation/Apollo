/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmailk.com
 */
@Slf4j
@Singleton
public class ShardDownloader {

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 20;
    private final FileDownloader fileDownloader;
    private Set<String> additionalPeers;
    private UUID myChainId;
    private Map<Long, Set<ShardInfo>> sortedShards;
    private javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    private ShardNameHelper shardNameHelper = new ShardNameHelper();
    private DirProvider dirProvider;

    @Inject
    public ShardDownloader(FileDownloader fileDownloader,
                           ChainsConfigHolder chainsConfig,
                           DirProvider dirProvider,
                           javax.enterprise.event.Event<ShardPresentData> presentDataEvent) {
        Objects.requireNonNull( chainsConfig, "chainConfig is NULL");
        this.myChainId = Objects.requireNonNull( chainsConfig.getActiveChain().getChainId(), "chainId is NULL");
        this.additionalPeers =  new HashSet<>();
        this.fileDownloader = Objects.requireNonNull( fileDownloader, "fileDownloader is NULL");
        this.sortedShards = new HashMap<>();
        this.dirProvider = Objects.requireNonNull( dirProvider, "dirProvider is NULL");
        this.presentDataEvent = Objects.requireNonNull(presentDataEvent, "presentDataEvent is NULL");
    }

    private boolean processPeerShardInfo(Peer p) {
        boolean haveShard = false;
        PeerClient pc = new PeerClient(p);
        ShardingInfo si = pc.getShardingInfo();
        additionalPeers.addAll(si.knownPeers);
        for (ShardInfo s : si.shards) {
            if (myChainId.equals(UUID.fromString(s.chainId))) {
                haveShard = true;
                si.source = p.getAnnouncedAddress();
                Set<ShardInfo> rs = sortedShards.get(s.shardId);
                if (rs == null) {
                    rs = new HashSet<>();
                    sortedShards.put(s.shardId, rs);
                }
                rs.add(s);
            }
        }
        return haveShard;
    }

    public Map<Long, Set<ShardInfo>> getShardInfoFromPeers() {
        int counter = 0;

        Set<Peer> knownPeers = fileDownloader.getAllAvailablePeers();
        //get sharding info from known peers
        for (Peer p : knownPeers) {
            if (processPeerShardInfo(p)) {
                counter++;
            }
            if (counter > ENOUGH_PEERS_FOR_SHARD_INFO) {
                break;
            }
        }
        //we have not enough known peers, connect to additional
        if (counter < ENOUGH_PEERS_FOR_SHARD_INFO) {
            for (String pa : additionalPeers) {
                Peer p = Peers.findOrCreatePeer(pa, true);
                if (processPeerShardInfo(p)) {
                    counter++;
                }
                if (counter > ENOUGH_PEERS_FOR_SHARD_INFO) {
                    break;
                }
            }
        }
        return sortedShards;
    }

    public FileDownloadDecision prepareAndStartDownload() {
        log.debug("prepareAndStartDownload...");
        FileDownloadDecision result = FileDownloadDecision.NotReady;
        if (sortedShards.isEmpty()) {
            getShardInfoFromPeers();
            log.debug("Shards received from Peers '{}'", sortedShards);
        }
        if (sortedShards.isEmpty()) {
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            ShardPresentData shardPresentData = new ShardPresentData();
            presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored            
            
        } else {
            //we have some shards available on the networks, let's decide what to do
            List<Long> shardIds = new ArrayList(sortedShards.entrySet());
            Collections.sort(shardIds);
            Long lastShard = shardIds.get(shardIds.size() - 1);
            log.debug("Last known ShardId '{}'", lastShard);
            String fileID = shardNameHelper.getShardNameByShardId(lastShard, myChainId);
            fileDownloader.setFileId(fileID);
            // check if zip file exists on local node
            String zipFileName = shardNameHelper.getShardArchiveNameByShardId(lastShard, myChainId);
            File zipInExportedFolder = dirProvider.getDataExportDir().resolve(zipFileName).toFile();
            if (zipInExportedFolder.exists()) {
                log.info("No need to download '{}'  as it is found in folder = '{}'", zipFileName,
                        dirProvider.getDataExportDir());
                result = FileDownloadDecision.OK;
                return result;
            }
            // prepare downloading
            log.debug("Start preparation to downloading ");
            result = fileDownloader.prepareForDownloading();
            if( result == FileDownloadDecision.AbsOK
                    || result == FileDownloadDecision.OK
                    || result == FileDownloadDecision.Risky ) {
                log.debug("Starting shard downloading: '{}'", fileID);
                fileDownloader.startDownload();
                //TODO: how to file event when file is downloaded and OK?
            }else{
                log.debug("Can not find enough peers with good shard: {}", fileID);
                //FIRE event when shard is NOT PRESENT
                ShardPresentData shardPresentData = new ShardPresentData();
                presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored                 
            }
        }
        return result;
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
