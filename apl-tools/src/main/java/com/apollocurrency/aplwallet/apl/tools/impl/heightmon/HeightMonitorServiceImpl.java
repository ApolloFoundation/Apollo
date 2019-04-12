/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.HeightMonitorConfig;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.NetworkStats;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerInfo;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerDiffStat;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeersConfig;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

@Singleton
public class HeightMonitorServiceImpl implements HeightMonitorService {

    private static final Logger log = getLogger(HeightMonitorServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<Integer> DEFAULT_PERIODS = List.of(1, 2, 4, 6, 8, 12, 24, 48, 96);
    private static final int CONNECT_TIMEOUT = 3_000;
    private static final int IDLE_TIMEOUT = 2_000;
    private static final int BLOCKS_TO_RETRIEVE = 1000;
    private static final int DEFAULT_PORT = 7876;
    private static final String URL_FORMAT = "%s://%s:%d/apl";
    private final AtomicReference<NetworkStats> lastStats = new AtomicReference<>();

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpClient client;
    private List<PeerInfo> peers;
    private List<MaxBlocksDiffCounter> maxBlocksDiffCounters;
    private int port;
    private List<PeerInfo> peerApiUrls;

    public HeightMonitorServiceImpl() {
    }

    @PostConstruct
    public void init() {
        client = createHttpClient();
        try {
            client.start();
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setUp(HeightMonitorConfig config) {
        PeersConfig peersConfig = config.getPeersConfig();
        this.peers = Collections.synchronizedList(peersConfig.getPeersInfo().stream().peek(p-> {
            if (p.getPort() == null) {
                p.setPort(peersConfig.getDefaultPort());
            }
        }).collect(Collectors.toList()));
        this.maxBlocksDiffCounters = createMaxBlocksDiffCounters(config.getMaxBlocksDiffPeriods() == null ? DEFAULT_PERIODS : config.getMaxBlocksDiffPeriods());
        this.port = config.getPort() == null ? DEFAULT_PORT : config.getPort();
        this.peerApiUrls = this.peers.stream().map(this::createUrl)
                .map(peer -> String.format(URL_FORMAT, peer, this.port))
                .collect(Collectors.toList());
    }

    @Override
    public boolean addPeer(String address, Integer port, Boolean useHttps) throws UnknownHostException {
        StringValidator.requireNonBlank(address);
        InetAddress inetAddress = InetAddress.getByName(address);
        peerIps.add(inetAddress.getHostName());
        if (port == null) {
            port = this.port;
        }
        if (port <= 0) {
            throw new IllegalArgumentException("Port cannot be less or equal to 0");
        }
        if (useHttps == null) {
            useHttps = false;
        }
        createUrl(inetAddress.getHostName(), port, useHttps);

        //        peerApiUrls.add();
        return true;
    }

    private String createUrl(PeerInfo peerInfo) {
        return new PeerInfo(String.format(URL_FORMAT))
    }

    public void addNewPeer(String ip) {

    }


    @PreDestroy
    public void shutdown() {
        try {
            client.stop();
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

    public NetworkStats updateStats() {
        log.info("===========================================");
        Map<String, List<Block>> peerBlocks = getPeersBlocks();
        log.info(String.format("%5.5s %5.5s %-12.12s %-12.12s %7.7s %7.7s", "diff1","diff2", "peer1", "peer2", "height1", "height2"));
        int currentMaxBlocksDiff = -1;
        NetworkStats networkStats = new NetworkStats();
        for (int i = 0; i < peerIps.size(); i++) {
            List<Block> targetBlocks = peerBlocks.get(peerIps.get(i));
            if (targetBlocks.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < peerIps.size(); j++) {
                List<Block> blocksToCompare = peerBlocks.get(peerIps.get(j));
                Block lastMutualBlock = findLastMutualBlock(blocksToCompare, targetBlocks);
                if (!blocksToCompare.isEmpty()) {
                    int lastHeight = targetBlocks.get(0).getHeight();
                    int blocksDiff1 = getBlockDiff(lastMutualBlock, lastHeight);
                    int blocksDiff2 = getBlockDiff(lastMutualBlock, blocksToCompare.get(0).getHeight());
                    currentMaxBlocksDiff = Math.max(blocksDiff1, currentMaxBlocksDiff);
                    log.info(String.format("%5d %5d %-12.12s %-12.12s %7d %7d", blocksDiff1, blocksDiff2, peerIps.get(i), peerIps.get(j), lastHeight, blocksToCompare.get(0).getHeight()));
                    networkStats.getPeerDiffStats().add(new PeerDiffStat(blocksDiff1, blocksDiff2, peerIps.get(i), peerIps.get(j), lastHeight, blocksToCompare.get(0).getHeight()));
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

    private int getBlockDiff(Block lastMutualBlock, int lastHeight) {
        int blocksDiff = -1;
        if (lastMutualBlock != null) {
            int mutualBlockHeight = lastMutualBlock.getHeight();
            blocksDiff = lastHeight - mutualBlockHeight;
        }
        return blocksDiff;
    }

    private Map<String, List<Block>> getPeersBlocks() {
        Map<String, List<Block>> peerBlocks = new HashMap<>();
        List<CompletableFuture<List<Block>>> getBlocksRequests = new ArrayList<>();
        for (String peerUrl : peerApiUrls) {
            getBlocksRequests.add(CompletableFuture.supplyAsync(() -> getBlocksList(peerUrl)));
        }
        for (int i = 0; i < getBlocksRequests.size(); i++) {
            try {
                peerBlocks.put(peerIps.get(i), getBlocksRequests.get(i).get());
            }
            catch (InterruptedException | ExecutionException e) {
                log.error("Error getting blocks for " + peerIps.get(i), e);
            }
        }
        return peerBlocks;
    }

    private String getBlocksJson(String peerUrl, int firstIndex) throws InterruptedException, ExecutionException, TimeoutException {
        Request request = client.newRequest(peerUrl)
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

