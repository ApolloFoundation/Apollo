/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;

public class HeightMonitor {
    private static final Logger log = getLogger(HeightMonitor.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_PEERS_FILE = "peers.txt";
    private static final List<Integer> DEFAULT_PERIODS = Collections.unmodifiableList(Arrays.asList(1, 2, 4, 6, 8, 12, 24, 48, 96));
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int DEFAULT_DELAY = 30;

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpClient client;
    private List<String> peerIps;
    private List<MaxBlocksDiffCounter> maxBlocksDiffCounters;
    private ScheduledExecutorService executor;
    private int delay;

    public HeightMonitor(List<String> peerIps, List<Integer> maxBlocksDiffPeriods, int delay) {
        this.client = createHttpClient();
        this.peerIps = peerIps;
        this.executor = Executors.newScheduledThreadPool(1);
        this.maxBlocksDiffCounters = createMaxBlocksDiffCounters(maxBlocksDiffPeriods);
        this.delay = delay;
    }

    private HttpClient createHttpClient() {
        HttpClient httpClient = new HttpClient();
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
        System.out.println("Start iteration");
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
        List<String> peersUrls = peerIps.stream().map(peer -> "http://" + peer + ":6876/apl").collect(Collectors.toList());
        Map<String, List<Block>> peerBlocks = new HashMap<>();
        for (int i = 0; i < peersUrls.size(); i++) {
            List<Block> blocksList = getBlocksList(peersUrls.get(i));
            peerBlocks.put(peerIps.get(i), blocksList);
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
            log.error("Cannot perform request to {} - ", peerUrl, e.toString());
        }
        catch (IOException e) {
            log.error("Cannot read json as block list ", e);
        }
        return blocks;

    }


    private class MaxBlocksDiffCounter {
        private int period;
        private int value;
        private long lastResetTime = System.currentTimeMillis() / (1000 * 60);

        private MaxBlocksDiffCounter(int period) {
            this.period = period;
        }

        private void update(int currentBlockDiff) {
            value = Math.max(value, currentBlockDiff);
            log.info("MAX Blocks diff for last {}h is {} blocks", period, value);
            long currentTime = System.currentTimeMillis() / 1000 / 60;
            if (currentTime - lastResetTime >= period * 60) {
                lastResetTime = currentTime;
                value = currentBlockDiff;
            }
        }
    }



    private static List<String> readPeers(String arg) {
        URL resource = HeightMonitor.class.getClassLoader().getResource(arg);
        try {
            Objects.requireNonNull(resource, "Resource of " + arg + " cannot be null");
            List<String> peers = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    peers.add(line);
                }
            }
            return peers;
        }
        catch (IOException e) {
            System.out.println("Cannot read file " + arg);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static Block findLastMutualBlock(List<Block> blocksToCompare, List<Block> targetBlocks) {
        for (Block block : blocksToCompare) {
            if (targetBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }
    
    public static HeightMonitor create(String peersFile, List<Integer> periods, Integer delay){    
        List<String> peers = readPeers(!peersFile.isEmpty()? peersFile : DEFAULT_PEERS_FILE);
        if(periods==null||periods.isEmpty()){
            periods=DEFAULT_PERIODS;
        }
        if(delay==null){
            delay= DEFAULT_DELAY;
        }
        HeightMonitor heightMonitor = new HeightMonitor(peers, periods, delay);
        Runtime.getRuntime().addShutdownHook(new Thread(heightMonitor::stop));
        return heightMonitor;
    }
    
    private static class Block {
        private long id;
        private int height;
        private int timestamp;
        private int timeout;
        private int version;
        private long generatorId;

        @Override
        public String toString() {
            return "Block{" +
                    "id=" + id +
                    ", height=" + height +
                    ", timestamp=" + timestamp +
                    ", timeout=" + timeout +
                    ", version=" + version +
                    ", generatorId=" + generatorId +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Block)) return false;
            Block block = (Block) o;
            return id == block.id &&
                    height == block.height &&
                    timestamp == block.timestamp &&
                    timeout == block.timeout &&
                    version == block.version &&
                    generatorId == block.generatorId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, height, timestamp, timeout, version, generatorId);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public long getGeneratorId() {
            return generatorId;
        }

        public void setGeneratorId(long generatorId) {
            this.generatorId = generatorId;
        }

        public Block() {
        }

        public Block(long id, int height, int timestamp, int timeout, int version, long generatorId) {
            this.id = id;
            this.height = height;
            this.timestamp = timestamp;
            this.timeout = timeout;
            this.version = version;
            this.generatorId = generatorId;
        }
    }
}
