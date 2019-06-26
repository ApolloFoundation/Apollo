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
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
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

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 6; //6 threads is enough for downloading
    private final static int ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL = 20; // question 20 peers and surrender
    private final FileDownloader fileDownloader;
    private Set<String> additionalPeers;
    private UUID myChainId;
    private Map<Long, Set<ShardInfo>> sortedShards;
    private Map<Long, Set<Peer>> shardsPeers;
    private javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    private ShardNameHelper shardNameHelper = new ShardNameHelper();
    private DirProvider dirProvider;

    @Inject
    public ShardDownloader(FileDownloader fileDownloader,
            BlockchainConfig blockchainConfig,
            DirProvider dirProvider,
            javax.enterprise.event.Event<ShardPresentData> presentDataEvent) {
        Objects.requireNonNull(blockchainConfig, "chainId is NULL");
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.additionalPeers = Collections.synchronizedSet(new HashSet<>());
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader is NULL");
        this.sortedShards = Collections.synchronizedMap(new HashMap<>());
        this.shardsPeers = Collections.synchronizedMap(new HashMap<>());
        this.dirProvider = Objects.requireNonNull(dirProvider, "dirProvider is NULL");
        this.presentDataEvent = Objects.requireNonNull(presentDataEvent, "presentDataEvent is NULL");
    }

    private boolean processPeerShardInfo(Peer p) {
        boolean haveShard = false;
        PeerClient pc = new PeerClient(p);
        ShardingInfo si = pc.getShardingInfo();
        log.trace("shardInfo = {}", si);
        if (si != null) {
            additionalPeers.addAll(si.knownPeers);
            for (ShardInfo s : si.shards) {
                if (myChainId.equals(UUID.fromString(s.chainId))) {
                    haveShard = true;
                    si.source = p.getAnnouncedAddress();
                    synchronized (this) {
                        Set<Peer> ps = shardsPeers.get(s.shardId);
                        if (ps == null) {
                            ps = new HashSet<>();
                            shardsPeers.put(s.shardId, ps);
                        }
                        ps.add(p);

                        Set<ShardInfo> rs = sortedShards.get(s.shardId);
                        if (rs == null) {
                            rs = new HashSet<>();
                            sortedShards.put(s.shardId, rs);
                        }
                        rs.add(s);
                    }
                }
            }
        }
        return haveShard;
    }

    public Map<Long, Set<ShardInfo>> getShardInfoFromPeers() {
        log.debug("Request ShardInfo from Peers...");
        int counterWinShardInfo = 0;
        int counterTotal = 0;
        Set<Peer> knownPeers = fileDownloader.getAllAvailablePeers();
        log.debug("ShardInfo knownPeers {}", knownPeers);
        //get sharding info from known peers
        for (Peer p : knownPeers) {
            if (processPeerShardInfo(p)) {
                counterWinShardInfo++;
            }
            if (counterWinShardInfo > ENOUGH_PEERS_FOR_SHARD_INFO) {
                log.debug("counter > ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                break;
            }
            counterTotal++;
            if (counterTotal > ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                break;
            }
        }
        //we have not enough known peers, connect to additional
        if (counterWinShardInfo < ENOUGH_PEERS_FOR_SHARD_INFO) {
            Set<String> additionalPeersCopy = new HashSet<>();
            additionalPeersCopy.addAll(additionalPeers);
            //avoid modification while iterating
            for (String pa : additionalPeersCopy) {

                Peer p = Peers.findOrCreatePeer(pa, true);
                if (processPeerShardInfo(p)) {
                    counterWinShardInfo++;
                }
                if (counterWinShardInfo > ENOUGH_PEERS_FOR_SHARD_INFO) {
                    log.debug("counter > ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                    break;
                }
                counterTotal++;
                if (counterTotal > ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                    break;
                }
            }
        }
        log.debug("Request ShardInfo result {}", sortedShards);
        return sortedShards;
    }

    public FileDownloadDecision prepareAndStartDownload() {
        log.debug("prepareAndStartDownload...");
        FileDownloadDecision result = FileDownloadDecision.NotReady;
        if (sortedShards.isEmpty()) { //???
            getShardInfoFromPeers();
            log.debug("Shards received from Peers '{}'", sortedShards);
        }
        if (sortedShards.isEmpty()) {
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("result = {}, Fire = {}", result, "NO_SHARD");
            ShardPresentData shardPresentData = new ShardPresentData();
            presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored
            return result;
        } else {
            //we have some shards available on the networks, let's decide what to do
            List<Long> shardIds = new ArrayList(sortedShards.keySet());
            Collections.sort(shardIds);
            Long lastShard = shardIds.get(shardIds.size() - 1);
            log.debug("Last known ShardId '{}'", lastShard);            
            String fileID = shardNameHelper.getFullShardId(lastShard, myChainId);
            log.debug("fileID = '{}'", fileID);
            fileDownloader.setFileId(fileID);
            // check if zip file exists on local node
            String zipFileName = shardNameHelper.getShardArchiveNameByShardId(lastShard, myChainId);
            File zipInExportedFolder = dirProvider.getDataExportDir().resolve(zipFileName).toFile();
            log.debug("Checking existence zip = '{}', ? = {}", zipInExportedFolder, zipInExportedFolder.exists());
            if (zipInExportedFolder.exists()) {
                log.info("No need to download '{}'  as it is found in folder = '{}'", zipFileName,
                        dirProvider.getDataExportDir());
                result = FileDownloadDecision.OK;
                return result;
            }
            // prepare downloading
            Set<Peer> lastShardPeers = shardsPeers.get(lastShard);
            if(lastShardPeers.size()<2){ //we cannot useStudent's distribution with 1
              result = FileDownloadDecision.NoPeers;
             //FIRE event when shard is NOT PRESENT
              log.debug("Less then 2 peers. result = {}, Fire = {}", result, "NO_SHARD");
              ShardPresentData shardPresentData = new ShardPresentData();
              presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored
              return result;
            }
            log.debug("Start preparation to downloading...");
            result = fileDownloader.prepareForDownloading(lastShardPeers);
            if (result == FileDownloadDecision.AbsOK
                    || result == FileDownloadDecision.OK
                    || result == FileDownloadDecision.Risky) {
                log.debug("Starting shard downloading: '{}'", fileID);
                fileDownloader.startDownload();
                //see FileDownloader::getNextEmptyChunk() for sucess event
            } else {
                log.error("Can not find enough peers with good shard: '{}' because result '{}'", fileID, result);
                // We CAN'T download latest SHARD archive, start from the beginning - FIRE event here
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
