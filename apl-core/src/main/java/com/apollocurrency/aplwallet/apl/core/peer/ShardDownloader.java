/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.HasHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerShardInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Instance;
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

/**
 *
 * @author alukin@gmailk.com
 */
@Slf4j
@Singleton
public class ShardDownloader {

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 6; //6 threads is enough for downloading
    private final static int ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL = 20; // question 20 peers and surrender
    private final Set<String> additionalPeers;
    private final UUID myChainId;
    private final Map<Long, Set<ShardInfo>> sortedShards;
    private final Map<Long, Set<Peer>> shardsPeers;
    private final Map<String,ShardingInfo> shardInfoByPeers;
    private final javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    private final ShardNameHelper shardNameHelper = new ShardNameHelper();
    private final DownloadableFilesManager downloadableFilesManager;
    Map<Long,List<HasHashSum>> goodPeersMap=new HashMap<>();
    Map<Long,List<HasHashSum>> badPeersMap=new HashMap<>();

    private final Instance<FileDownloader> fileDownloaders;
    private final PropertiesHolder propertiesHolder;
    private final PeersService peers;
    
    @Inject
    public ShardDownloader(Instance<FileDownloader> fileDownloaders,
                           BlockchainConfig blockchainConfig,
                           DownloadableFilesManager downloadableFilesManager,
                           javax.enterprise.event.Event<ShardPresentData> presentDataEvent,
                           PropertiesHolder propertiesHolder,
                           PeersService peers) {
        Objects.requireNonNull(blockchainConfig, "chainId is NULL");
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.additionalPeers = Collections.synchronizedSet(new HashSet<>());
        this.sortedShards = Collections.synchronizedMap(new HashMap<>());
        this.shardsPeers = Collections.synchronizedMap(new HashMap<>());
        this.shardInfoByPeers = Collections.synchronizedMap(new HashMap<>());
        this.downloadableFilesManager = Objects.requireNonNull(downloadableFilesManager, "downloadableFilesManager is NULL");
        this.presentDataEvent = Objects.requireNonNull(presentDataEvent, "presentDataEvent is NULL");
        this.fileDownloaders=fileDownloaders;
        this.propertiesHolder=propertiesHolder;
        this.peers = peers;
    }

    private boolean processPeerShardInfo(Peer p) {
        boolean haveShard = false;
        PeerClient pc = new PeerClient(p);
        ShardingInfo si = pc.getShardingInfo();
        log.trace("shardInfo = {}", si);
        if (si != null) {
            shardInfoByPeers.put(p.getHostWithPort(), si);
            si.source = p.getHostWithPort();
            additionalPeers.addAll(si.knownPeers);
            for (ShardInfo s : si.shards) {
                if (myChainId.equals(UUID.fromString(s.chainId))) {
                    haveShard = true;
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
        FileDownloader fileDownloader = fileDownloaders.get();
        Set<Peer> knownPeers = fileDownloader.getAllAvailablePeers();
        log.trace("ShardInfo knownPeers {}", knownPeers);
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

                Peer p = peers.findOrCreatePeer(null, pa, true);
                if(p!=null) {
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
                }else{
                    log.debug("Can not create peer: {}",pa);
                }
            }
        }
        log.debug("Request ShardInfo result {}", sortedShards);
        return sortedShards;
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

    private byte[] getHash(long shardId, String peerAddr) {
        byte[] res = null;
        ShardingInfo psi = shardInfoByPeers.get(peerAddr);
        if(psi!=null){
            for(ShardInfo si: psi.shards){
                if(myChainId.equals(UUID.fromString(si.chainId))
                        && si.shardId==shardId)
                {
                   res = Convert.parseHexString(si.zipCrcHash);
                   break;
                }
            }
        }
        return res;
    }

    private FileDownloadDecision checkShard(Long shardId, Set<Peer> shardPeers) {
        //do statistical analysys of shard's hashes
        PeersList<PeerShardInfo> shardPeerList = new PeersList<>();
        for (Peer p : shardPeers) {
            PeerShardInfo psi = new PeerShardInfo(new PeerClient(p), shardId, myChainId);
            psi.setHash(getHash(shardId, p.getHostWithPort()));
            shardPeerList.add(psi);
        }
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(shardPeerList);
        FileDownloadDecision res = pvdm.calcualteNetworkState();
        goodPeersMap.put(shardId,pvdm.getValidPeers());
        badPeersMap.put(shardId,pvdm.getInvalidPeers());
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeersMap.get(shardId), badPeersMap.get(shardId));

        return res;
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

    private boolean isAcceptable(FileDownloadDecision d) {
        boolean res = (d == FileDownloadDecision.AbsOK || d == FileDownloadDecision.OK );
        return res;
    }

    public FileDownloadDecision tryDownloadShard(Long shardId) {
        FileDownloadDecision result;
        log.debug("Processing shardId '{}'", shardId);
        // chek before downloading
        Set<Peer> thisShardPeers = shardsPeers.get(shardId);
        if (thisShardPeers.size() < 2) { //we cannot use Student's T distribution with 1 sample
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("Less then 2 peers. result = {}, Fire = {}", result, "NO_SHARD");
            return result;
        }
        result = checkShard(shardId, thisShardPeers);
        if (!isAcceptable(result)) {
            log.warn("Shard {} can not be loaded from peers", shardId);
            return result;
        }
        // check if zip file exists on local node
        byte[] goodHash = goodPeersMap.get(shardId).get(0).getHash();
        if (checkShardDownloadedAlready(shardId, goodHash)) {
            fireShardPresentEvent(shardId);
            result = FileDownloadDecision.OK;
            return result;
        }
        log.debug("Start preparation to downloading...");
        FileDownloader fileDownloader = fileDownloaders.get();
        String fileID = shardNameHelper.getFullShardId(shardId, myChainId);
        log.debug("fileID = '{}'", fileID);
        fileDownloader.setFileId(fileID);
        result = fileDownloader.prepareForDownloading(thisShardPeers);
        if (isAcceptable(result)) {
            log.debug("Starting shard downloading: '{}'", fileID);
            fileDownloader.startDownload();
            //see FileDownloader::getNextEmptyChunk() for sucess event emition
        } else {
            log.warn("Can not find enough peers with good shard: '{}' because result '{}'", fileID, result);
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
        if (sortedShards.isEmpty()) { //???
            getShardInfoFromPeers();
            log.debug("Shards received from Peers '{}'", sortedShards);
        }
        if (sortedShards.isEmpty()) {
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("result = {}, Fire = {}", result, "NO_SHARD");
            fireNoShardEvent();
            return result;
        } else {
            //we have some shards available on the networks, let's decide what to do
            List<Long> shardIds = new ArrayList(sortedShards.keySet());
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

    private AnnotationLiteral<ShardPresentEvent> literal(ShardPresentEventType shardPresentEventType) {
        return new ShardPresentEventBinding() {
            @Override
            public ShardPresentEventType value() {
                return shardPresentEventType;
            }
        };
    }
}
