/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.HeightMonitorConfig;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.NetworkStats;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerDiffStat;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerInfo;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerMonitoringResult;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeersConfig;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.ShardDTO;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class HeightMonitorServiceImpl implements HeightMonitorService {

    private static final Logger log = getLogger(HeightMonitorServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<Integer> DEFAULT_PERIODS = List.of(1, 2, 4, 6, 8, 12, 24, 48, 96);
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int IDLE_TIMEOUT = 5_000;
    private static final int BLOCKS_TO_RETRIEVE = 1000;
    private static final Version DEFAULT_VERSION = new Version("0.0.0");
    private static final String URL_FORMAT = "%s://%s:%d";
    private final AtomicReference<NetworkStats> lastStats = new AtomicReference<>();

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpClient client;
    private List<PeerInfo> peers;
    private List<MaxBlocksDiffCounter> maxBlocksDiffCounters;
    private int port;
    private List<String> peerApiUrls;
    private ExecutorService executor;

    public HeightMonitorServiceImpl() {}

    @PostConstruct
    public void init() {
        client = createHttpClient();
        try {
            client.start();
            executor = Executors.newFixedThreadPool(30);
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @Override
    public List<PeerInfo> getAllPeers() {
        return peers;
    }

    @Override
    public void setUp(HeightMonitorConfig config) {
        PeersConfig peersConfig = config.getPeersConfig();
        this.port = peersConfig.getDefaultPort();
        this.peers = Collections.synchronizedList(peersConfig.getPeersInfo().stream().peek(this::setDefaultPortIfNull).collect(Collectors.toList()));
        this.maxBlocksDiffCounters = createMaxBlocksDiffCounters(config.getMaxBlocksDiffPeriods() == null ? DEFAULT_PERIODS : config.getMaxBlocksDiffPeriods());
        this.peerApiUrls = Collections.synchronizedList(this.peers.stream().map(this::createUrl).collect(Collectors.toList()));

    }

    private PeerInfo setDefaultPortIfNull(PeerInfo peerInfo) {
        if (peerInfo.getPort() == null) {
            peerInfo.setPort(port);
        }
        return peerInfo;
    }

    @Override
    public boolean addPeer(PeerInfo peer) {
        Objects.requireNonNull(peer);
        setDefaultPortIfNull(peer);
        String url = createUrl(peer);
        boolean result = false;
        if (!peers.contains(peer)) {
            result = true;
            peers.add(peer);
            peerApiUrls.add(url);
            log.info("Added new peer: {}", peer.getHost());
        }
        return result;
    }

    private String createUrl(PeerInfo peerInfo) {
        return String.format(URL_FORMAT, peerInfo.getSchema(), peerInfo.getHost(), peerInfo.getPort());
    }


    @PreDestroy
    public void shutdown() {
        try {
            client.stop();
            executor.shutdownNow();
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private HttpClient createHttpClient() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setIdleTimeout(IDLE_TIMEOUT);
        httpClient.setConnectTimeout(CONNECT_TIMEOUT);
        httpClient.setFollowRedirects(false);
        return httpClient;
    }

    private List<MaxBlocksDiffCounter> createMaxBlocksDiffCounters(List<Integer> maxBlocksDiffPeriods) {
        return maxBlocksDiffPeriods.stream().map(MaxBlocksDiffCounter::new).collect(Collectors.toList());
    }

    public NetworkStats getLastStats() {
        return lastStats.get();
    }

    @Override
    public NetworkStats updateStats() {
        log.info("===========================================");
        Map<String, PeerMonitoringResult> peerBlocks = getPeersMonitoringResults();
        NetworkStats networkStats = new NetworkStats();
        peerBlocks.forEach((peer, result)-> {
            List<String> shardList = result.getShards().stream().map(this::getShardHashFormatted).collect(Collectors.toList());
            log.info(String.format("%-16.16s - %8d - %s", peer, result.getHeight(), String.join("->", shardList)));
            networkStats.getPeerHeight().put(peer, result.getHeight());
            networkStats.getPeerShards().put(peer, shardList);
        });
        log.info(String.format("%5.5s %5.5s %-16.16s %-16.16s %9.9s %7.7s %7.7s %8.8s %8.8s %-13.13s %-13.13s %13.13s", "diff1", "diff2", "peer1", "peer2", "milestone", "height1", "height2", "version1", "version2", "shard1", "shard2", "shard-status"));
        int currentMaxBlocksDiff = -1;
        for (int i = 0; i < peers.size(); i++) {
            String host1 = peers.get(i).getHost();
            PeerMonitoringResult targetMonitoringResult = peerBlocks.get(host1);
            List<Block> targetBlocks = targetMonitoringResult.getBlocks();
            if (targetBlocks.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < peers.size(); j++) {
                String host2 = peers.get(j).getHost();
                PeerMonitoringResult comparedMonitoringResult = peerBlocks.get(host2);
                List<Block> blocksToCompare = comparedMonitoringResult.getBlocks();
                Block lastMutualBlock = findLastMutualBlock(blocksToCompare, targetBlocks);
                if (!blocksToCompare.isEmpty()) {
                    int lastHeight = targetBlocks.get(0).getHeight();
                    int blocksDiff1 = getBlockDiff(lastMutualBlock, lastHeight);
                    int blocksDiff2 = getBlockDiff(lastMutualBlock, blocksToCompare.get(0).getHeight());
                    int milestoneHeight = getMilestoneHeight(lastMutualBlock);
                    String shardsStatus = getShardsStatus(targetMonitoringResult, comparedMonitoringResult);
                    String shard1 = getShardOrNothing(targetMonitoringResult);
                    String shard2 = getShardOrNothing(comparedMonitoringResult);
                    currentMaxBlocksDiff = Math.max(blocksDiff1, currentMaxBlocksDiff);
                    log.info(String.format("%5d %5d %-16.16s %-16.16s %9d %7d %7d %8.8s %8.8s %-13.13s %-13.13s %13.13s", blocksDiff1, blocksDiff2, host1, host2, milestoneHeight, lastHeight, blocksToCompare.get(0).getHeight(), targetMonitoringResult.getVersion(), comparedMonitoringResult.getVersion(), shard1, shard2, shardsStatus));
                    networkStats.getPeerDiffStats().add(new PeerDiffStat(blocksDiff1, blocksDiff2, host1, host2, lastHeight, milestoneHeight, blocksToCompare.get(0).getHeight(), targetMonitoringResult.getVersion(), comparedMonitoringResult.getVersion(), shard1, shard2, shardsStatus));
                }
            }
        }
        log.info("========Current max diff {} =========", currentMaxBlocksDiff);
        networkStats.setCurrentMaxDiff(currentMaxBlocksDiff);
        for (MaxBlocksDiffCounter maxBlocksDiffCounter : maxBlocksDiffCounters) {
            maxBlocksDiffCounter.update(currentMaxBlocksDiff);
            networkStats.getDiffForTime().put(maxBlocksDiffCounter.getPeriod(), maxBlocksDiffCounter.getValue());
        }
        lastStats.set(networkStats);
        return networkStats;
    }

    private String getShardOrNothing(PeerMonitoringResult targetMonitoringResult) {
        List<ShardDTO> shards = targetMonitoringResult.getShards();
        if (shards.isEmpty()) {
            return "---";
        } else {
            return getShardHashFormatted(shards.get(0));
        }
    }

    private String getShardHashFormatted(ShardDTO shardDTO) {
        String prunableZipHash = shardDTO.getPrunableZipHash();
        return shardDTO.getCoreZipHash().substring(0, 6) + ":" + (prunableZipHash != null ? prunableZipHash.substring(0, 6) : "");
    }

    private String getShardsStatus(PeerMonitoringResult targetMonitoringResult, PeerMonitoringResult comparedMonitoringResult) {
        List<ShardDTO> targetShards = targetMonitoringResult.getShards();
        List<ShardDTO> comparedShards = comparedMonitoringResult.getShards();
        String status = "OK";
        int comparedCounter = comparedShards.size() - 1;
        int targetCounter = targetShards.size() - 1;
        while (targetCounter >= 0 && comparedCounter >= 0) {
            if (!targetShards.get(targetCounter).equals(comparedShards.get(comparedCounter))) {
                status = "DIFF FROM " + targetShards.get(targetCounter).getShardId();
                break;
            }
            targetCounter--;
            comparedCounter--;
        }
        return status;
    }

    private int getMilestoneHeight(Block lastMutualBlock) {
        if (lastMutualBlock != null) {
            return lastMutualBlock.getHeight();
        } else {
            return -1;
        }
    }

    private int getBlockDiff(Block lastMutualBlock, int lastHeight) {
        int blocksDiff = -1;
        if (lastMutualBlock != null) {
            int mutualBlockHeight = lastMutualBlock.getHeight();
            blocksDiff = lastHeight - mutualBlockHeight;
        }
        return blocksDiff;
    }

    private Map<String, PeerMonitoringResult> getPeersMonitoringResults() {
        Map<String, PeerMonitoringResult> peerBlocks = new HashMap<>();
        List<CompletableFuture<PeerMonitoringResult>> getBlocksRequests = new ArrayList<>();
        for (String peerUrl : peerApiUrls) {
            getBlocksRequests.add(CompletableFuture.supplyAsync(() -> {
                List<Block> blocksList = getBlocksList(peerUrl);
                Version version = getPeerVersion(peerUrl);
                int height = getPeerHeight(peerUrl);
                List<ShardDTO> shards = getShards(peerUrl);
                return new PeerMonitoringResult(blocksList, version, height, shards);
            }, executor));
        }
        for (int i = 0; i < getBlocksRequests.size(); i++) {
            String host = peers.get(i).getHost();
            try {
                peerBlocks.put(host, getBlocksRequests.get(i).get());
            } catch (Exception e) {
                log.error("Error getting blocks for " + host, e);
            }
        }
        return peerBlocks;
    }

    private String getBlocksJson(String peerUrl, int firstIndex) throws InterruptedException, ExecutionException, TimeoutException {
        Request request = client.newRequest(peerUrl + "/apl")
                .method(HttpMethod.GET)
                .param("requestType", "getBlocks")
                .param("firstIndex", String.valueOf(firstIndex))
                .param("lastIndex", String.valueOf(firstIndex) + 99);
        ContentResponse response = request.send();
        if (response.getStatus() != HttpStatus.OK_200) {
            return "";
        }
        return response.getContentAsString();
    }

    private List<ShardDTO> getShards(String peerUrl) {
        List<ShardDTO> shards = new ArrayList<>();
        Request request = client.newRequest(peerUrl + "/rest/shards")
                .method(HttpMethod.GET);
        ContentResponse response;
        try {
            response = request.send();
            shards = objectMapper.readValue(response.getContentAsString(), new TypeReference<List<ShardDTO>>() {
            });
        } catch (InterruptedException | TimeoutException | ExecutionException | IOException e) {
            log.error("Unable to get or parse response from {} - {}", peerUrl, e.toString());
        } catch (Exception e) {
            log.error("Unknown exception:", e);
        }
        return shards;
    }

    private int getPeerHeight(String peerUrl) {
        int height = -1;
        Request request = client.newRequest(peerUrl + "/apl")
                .method(HttpMethod.GET)
                .param("requestType", "getBlock");
        ContentResponse response;
        try {
            response = request.send();
            JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
            height = jsonNode.get("height").asInt();
        } catch (InterruptedException | TimeoutException | ExecutionException | IOException e) {
            log.error("Unable to get or parse response from {} - {}", peerUrl, e.toString());
        } catch (Exception e) {
            log.error("Unknown exception:", e);
        }
        return height;
    }
    private Version getPeerVersion(String peerUrl) {
        Version res = DEFAULT_VERSION;
        Request request = client.newRequest(peerUrl + "/apl")
                .method(HttpMethod.GET)
                .param("requestType", "getBlockchainStatus");
        ContentResponse response = null;
        try {
            response = request.send();
            JsonNode jsonNode  = objectMapper.readTree(response.getContentAsString());
            res = new Version(jsonNode.get("version").asText());
        }
        catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Unable to get response from {} - {}", peerUrl, e.toString());
        }
        catch (IOException e) {
            log.error("Unable to parse peer version from json for {} - {}",peerUrl, e.toString());
        }
        return res;
    }

    private List<Block> getBlocksList(String peerUrl) {
        ArrayList<Block> blocks = new ArrayList<>();
        try {
            while (blocks.size() < BLOCKS_TO_RETRIEVE) {
                String blocksJson = getBlocksJson(peerUrl, blocks.size());
                JsonNode root = objectMapper.readTree(blocksJson);
                JsonNode blocksArray = root.get("blocks");
                List<Block> loadedBlocks = objectMapper.readValue(blocksArray.toString(), new TypeReference<List<Block>>() {});
                blocks.addAll(loadedBlocks);
                if (loadedBlocks.size() == 0) {
                    break;
                }
            }
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Cannot perform request to {} - {}", peerUrl, e.toString());
        }
        catch (IOException e) {
            log.error("Cannot read json as block list ", e);
        }
        catch (Throwable e) {
            log.error("Unable to get blocks from {} - {}", peerUrl, e.toString());
        }
        return blocks;

    }

    private Block findLastMutualBlock(List<Block> blocksToCompare, List<Block> targetBlocks) {
        for (Block block : blocksToCompare) {
            if (targetBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }

}

