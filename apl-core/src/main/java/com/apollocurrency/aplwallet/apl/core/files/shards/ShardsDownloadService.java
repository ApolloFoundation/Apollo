/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadEvent;
import com.apollocurrency.aplwallet.apl.core.files.FileDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.FileEventData;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for background downloading of shard files and related files
 *
 * @author alukin@gmail.com
 */
@Singleton
@Slf4j
public class ShardsDownloadService {

    public static final String FORCED_SHARD_ID_ENV = "APOLLO_FORCE_IMPORT_SHARD_ID";
    private static final int MIN_SHARDING_PEERS = 2;
    private final ShardInfoDownloader shardInfoDownloader;
    private final UUID myChainId;
    private final Event<ShardPresentData> presentDataEvent;
    private final FileDownloadService fileDownloadService;
    private final PropertiesHolder propertiesHolder;
    private final ShardNameHelper shardNameHelper = new ShardNameHelper();
    private final Map<Long, ShardDownloadStatus> shardDownloadStatuses = new HashMap<>();

    @Inject
    public ShardsDownloadService(ShardInfoDownloader shardInfoDownloader,
                                 BlockchainConfig blockchainConfig,
                                 Event<ShardPresentData> presentDataEvent,
                                 PropertiesHolder propertiesHolder,
                                 FileDownloadService fileDownloadService
    ) {
        this.shardInfoDownloader = shardInfoDownloader;
        this.fileDownloadService = fileDownloadService;
        this.myChainId = blockchainConfig.getChain().getChainId();
        this.presentDataEvent = presentDataEvent;
        this.propertiesHolder = propertiesHolder;
    }

    private static boolean isAcceptable(FileDownloadDecision d) {
        boolean res = (d == FileDownloadDecision.AbsOK || d == FileDownloadDecision.OK);
        return res;
    }

    public static Map<Long, Double> sortByValue(final Map<Long, Double> w) {
        return w.entrySet()
            .stream()
            .sorted((Map.Entry.<Long, Double>comparingByValue().reversed()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public boolean getShardingInfoFromPeers() {
        Map<String, ShardingInfo> shardInfoByPeers = shardInfoDownloader.getShardInfoFromPeers();
        if (shardInfoByPeers.size() < MIN_SHARDING_PEERS) {
            return false;
        }
        shardInfoDownloader.processAllPeersShardingInfo();
        Set<Long> shardIds = shardInfoDownloader.getSortedByIdShards().keySet();
        for (Long sId : shardIds) {
            ShardInfo si = shardInfoDownloader.getShardInfo(sId);
            if (si == null) {
                //very strange situation, but it could happend
                continue;
            }
            Set<String> shardFiles = new HashSet<>();
            shardFiles.add(shardNameHelper.getFullShardId(sId, myChainId));
            shardFiles.addAll(si.additionalFiles);
            ShardDownloadStatus st = new ShardDownloadStatus(shardFiles);
            shardDownloadStatuses.put(sId, st);
        }
        return shardInfoByPeers.size() >= MIN_SHARDING_PEERS;
    }

    public void onAnyFileDownloadEvent(@ObservesAsync @FileDownloadEvent FileEventData fileData) {
        //TODO: process events carefully
        for (Long shardId : shardDownloadStatuses.keySet()) {
            ShardDownloadStatus status = shardDownloadStatuses.get(shardId);
            if (fileData.fileOk) {
                status.setStatus(fileData.fileId, ShardDownloadStatus.OK);
            } else {
                status.setStatus(fileData.fileId, ShardDownloadStatus.FAILED);
                log.debug("File {} download failed. reason: {}", fileData.fileId, fileData.reason);
            }
            if (status.isDowloadedOK()) {
                if (!status.isSigalFired()) {
                    fireShardPresentEvent(shardId);
                    status.setSigalFired(true);
                }
            } else if (status.isDownloadCompleted()) {
                if (!status.isSigalFired()) {
                    fireNoShardEvent(shardId, "SHARDING: shard download failed");
                    status.setSigalFired(true);
                }
            }
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

    private void fireNoShardEvent(Long shardId, String reason) {
        ShardPresentData shardPresentData = new ShardPresentData();
        log.warn("Firing 'NO_SHARD' event. shard: {} reason: {}", shardId, reason);
        presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fire(shardPresentData); // data is ignored
    }

    private void fireShardPresentEvent(Long shardId) {
        ShardNameHelper snh = new ShardNameHelper();
        String fileId = snh.getFullShardId(shardId, myChainId);
        ShardInfo si = shardInfoDownloader.getShardInfo(shardId);

        if (si == null) {//forced shard import
            si = new ShardInfo(); //TODO: forced additional files
        }
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
     *
     * @param si shard info record
     * @return list of files we yet have to download from peers
     */
    private List<String> checkShardDownloadedAlready(ShardInfo si) {
        List<String> res = new ArrayList<>();
        // check if zip file exists on local node
        String shardFileId = shardNameHelper.getFullShardId(si.shardId, myChainId);
        if (!fileDownloadService.isFileDownloadedAlready(shardFileId, si.zipCrcHash)) {
            res.add(shardFileId);
        } else {
            log.debug("Shard {} already downloaded, fire SHARD_PRESENT event.", si);
            fireShardPresentEvent(si.shardId);
        }
        for (int i = 0; i < si.additionalFiles.size(); i++) {
            if (!fileDownloadService.isFileDownloadedAlready(si.additionalFiles.get(i), si.additionalHashes.get(i))) {
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
        filesToDownload.forEach((fileId) -> {
            fileDownloadService.startDownload(fileId, peers);
        });
        return result;
    }

    private Long readForcedShardId() {
        Long res = null;
        String envVal = System.getProperty(FORCED_SHARD_ID_ENV);
        if (envVal != null) {
            try {
                res = Long.parseLong(envVal);
            } catch (NumberFormatException ex) {
                log.debug("Invalid shard ID:{}", envVal);
            }
        }
        return res;
    }

    public FileDownloadDecision tryDownloadLastGoodShard() {
        boolean goodShardFound = false;
        log.debug("SHARDING: prepare and start downloading of last good shard in the network...");
        boolean doNotShardImport = propertiesHolder.getBooleanProperty("apl.noshardimport", false);
        FileDownloadDecision result = FileDownloadDecision.NotReady;
        if (doNotShardImport) {
            fireNoShardEvent(-1L, "SHARDING: skipping shard import due to config/command-line option");
            result = FileDownloadDecision.NoPeers;
            return result;
        }
        Long forcedShardImportId = readForcedShardId();
        if (forcedShardImportId != null) {
            log.debug("Defined {} to {}. Reading shard from disk", FORCED_SHARD_ID_ENV, forcedShardImportId);
            fireShardPresentEvent(forcedShardImportId);
            return FileDownloadDecision.AbsOK;
        }
        if (!getShardingInfoFromPeers()) {
            fireNoShardEvent(-1L, "SHARDING: no good shards foud in the network");
            result = FileDownloadDecision.NoPeers;
            return result;
        }
        if (shardInfoDownloader.getSortedByIdShards().isEmpty()) {
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("result = {}, Fire = {}", result, "NO_SHARD");
            fireNoShardEvent(-1L, "SHARDING: No good shards peers found");
            return result;
        } else {
            //we have some shards available on the networks, let's decide what to do
            Map<Long, Double> shardWeights = shardInfoDownloader.getShardRelativeWeights();
            shardWeights.keySet().forEach((k) -> {
                log.debug("Shard: {} Weight: {}", k, shardWeights.get(k));
            });
            for (Long shardId : sortByValue(shardWeights).keySet()) {
                double w = shardWeights.get(shardId);
                if (w > 0) {
                    result = tryDownloadShard(shardId);
                    goodShardFound = isAcceptable(result);
                    if (goodShardFound) {
                        break;
                    }
                }
            }
            if (!goodShardFound) {
                fireNoShardEvent(-1L, "SHARDING: No good shards found");
            }
        }
        return result;
    }

}
