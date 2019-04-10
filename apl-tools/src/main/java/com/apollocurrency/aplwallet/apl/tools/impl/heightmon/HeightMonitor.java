/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class HeightMonitor {
    private static final Logger log = getLogger(HeightMonitor.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<Integer> DEFAULT_PERIODS = List.of(1, 2, 4, 6, 8, 12, 24, 48, 96);
    private static final int CONNECT_TIMEOUT = 3_000;
    private static final int IDLE_TIMEOUT = 2_000;
    private static final int DEFAULT_DELAY = 30;
    private static final int DEFAULT_PORT = 7876;

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpClient client;
    private List<String> peerIps;
    private List<MaxBlocksDiffCounter> maxBlocksDiffCounters;
    private ScheduledExecutorService executor;
    private int delay;
    private int port;
    private List<String> peerApiUrls;

    public HeightMonitor(List<String> peerIps, List<Integer> maxBlocksDiffPeriods, Integer delay, Integer port) {
        this.client = createHttpClient();
        this.peerIps = peerIps;
        this.executor = Executors.newScheduledThreadPool(1);
        this.maxBlocksDiffCounters = createMaxBlocksDiffCounters(maxBlocksDiffPeriods == null ? DEFAULT_PERIODS : maxBlocksDiffPeriods);
        this.delay = delay == null ? DEFAULT_DELAY : delay;
        this.port = port == null ? DEFAULT_PORT : port;
        this.peerApiUrls = this.peerIps.stream().map(peer -> "http://" + peer.trim() + ":" + this.port + "/apl").collect(Collectors.toList());
    }

    private HttpClient createHttpClient() {
        HttpClient httpClient = new HttpClient();
        httpClient.setIdleTimeout(IDLE_TIMEOUT);
        httpClient.setConnectTimeout(CONNECT_TIMEOUT);
        return httpClient;
    }

    private List<MaxBlocksDiffCounter> createMaxBlocksDiffCounters(List<Integer> maxBlocksDiffPeriods) {
        return maxBlocksDiffPeriods.stream().map(MaxBlocksDiffCounter::new).collect(Collectors.toList());
    }

    public void start() {
        try {
            client.start();
            executor.scheduleWithFixedDelay(this::doBlockCompare, 0, delay, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            executor.shutdown();
            client.stop();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void doBlockCompare() {
        log.info("===========================================");
        Map<String, List<Block>> peerBlocks = getPeersBlocks();
        int currentMaxBlocksDiff = -1;
        for (int i = 0; i < peerIps.size(); i++) {
            List<Block> targetBlocks = peerBlocks.get(peerIps.get(i));
            for (int j = i + 1; j < peerIps.size(); j++) {
                List<Block> blocksToCompare = peerBlocks.get(peerIps.get(j));
                Block lastMutualBlock = findLastMutualBlock(blocksToCompare, targetBlocks);
                if (lastMutualBlock == null) {
                    log.error(" No mutual blocks between {} and {}", peerIps.get(i), peerIps.get(j));
                    continue;
                }
                int lastHeight = targetBlocks.get(0).getHeight();
                int mutualBlockHeight = lastMutualBlock.getHeight();
                int blocksDiff = lastHeight - mutualBlockHeight;
                currentMaxBlocksDiff = Math.max(blocksDiff, currentMaxBlocksDiff);

                log.info("Blocks diff is {} between peers {} and {}", blocksDiff, peerIps.get(i), peerIps.get(j));
            }
        }
        for (MaxBlocksDiffCounter maxBlocksDiffCounter : maxBlocksDiffCounters) {
            maxBlocksDiffCounter.update(currentMaxBlocksDiff);
        }
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

    private String getBlocksJson(String peerUrl) throws InterruptedException, ExecutionException, TimeoutException {
        Request request = client.newRequest(peerUrl)
                .method(HttpMethod.GET)
                .param("requestType", "getBlocks");
        ContentResponse response = request.send();
        if (response.getStatus() != HttpStatus.OK_200) {
            return "";
        }
        return response.getContentAsString();

    }

    private List<Block> getBlocksList(String peerUrl) {
        ArrayList<Block> blocks = new ArrayList<>();
        try {
            String blocksJson = getBlocksJson(peerUrl);
            JsonNode root = objectMapper.readTree(blocksJson);
            JsonNode blocksArray = root.get("blocks");
            blocks.addAll(objectMapper.readValue(blocksArray.toString(), new TypeReference<List<Block>>() {}));
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Cannot perform request to {} - {}", peerUrl, e.toString());
        }
        catch (IOException e) {
            log.error("Cannot read json as block list ", e);
        }
        catch (Throwable e) {
            log.error("Unable to get blocks from {}", peerUrl);
        }
        return blocks;

    }

    private static Block findLastMutualBlock(List<Block> blocksToCompare, List<Block> targetBlocks) {
        for (Block block : blocksToCompare) {
            if (targetBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }
}
