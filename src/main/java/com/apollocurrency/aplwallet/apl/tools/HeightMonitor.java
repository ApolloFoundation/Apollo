/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.LoggerFactory;

public class HeightMonitor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_LOG_FILE = "heightMonitor.log";
    private static final String DEFAULT_PEERS_FILE = "peers";
    private static final List<Integer> DEFAULT_PERIODS = Collections.unmodifiableList(Arrays.asList(1, 2, 4, 6, 8, 12, 24, 48, 96));
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int DEFAULT_DELAY = 30;

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpClient client;
    private List<String> peerIps;
    private Logger logger;
    private List<MaxBlocksDiffCounter> maxBlocksDiffCounters;
    private ScheduledExecutorService executor;
    private int delay;

    public HeightMonitor(List<String> peerIps, String logFile, List<Integer> maxBlocksDiffPeriods, int delay) {
        this.client = createHttpClient();
        this.peerIps = peerIps;
        this.logger = createLoggerFor(getClass(), logFile);
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
        Map<String, List<Block>> peerBlocks = getPeersBlocks();
        int currentMaxBlocksDiff = -1;
        for (int i = 0; i < peerIps.size(); i++) {
            List<Block> targetBlocks = peerBlocks.get(peerIps.get(i));
            for (int j = i + 1; j < peerIps.size(); j++) {
                List<Block> blocksToCompare = peerBlocks.get(peerIps.get(j));
                Block lastMutualBlock = findLastMutualBlock(blocksToCompare, targetBlocks);
                if (lastMutualBlock == null) {
                    logger.error(" No mutual blocks between {} and {}", peerIps.get(i), peerIps.get(j));
                    continue;
                }
                int lastHeight = targetBlocks.get(0).getHeight();
                int mutualBlockHeight = lastMutualBlock.getHeight();
                int blocksDiff = lastHeight - mutualBlockHeight;
                currentMaxBlocksDiff = Math.max(blocksDiff, currentMaxBlocksDiff);

                logger.info("Blocks diff is {} between peers {} and {}", blocksDiff, peerIps.get(i), peerIps.get(j));
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
            logger.error("Cannot perform request to {} - ", peerUrl, e.toString());
        }
        catch (IOException e) {
            logger.error("Cannot read json as block list ", e);
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
            logger.info("MAX Blocks diff for last {}h is {} blocks", period, value);
            long currentTime = System.currentTimeMillis() / 1000 / 60;
            if (currentTime - lastResetTime >= period * 60) {
                lastResetTime = currentTime;
                value = currentBlockDiff;
            }
        }
    }

    public static void main(String[] args) {
        List<String> peers = readPeers(args.length > 0 ? args[0] : DEFAULT_PEERS_FILE);

        String logFile =
                args.length > 1 ? args[1] != null && !args[1].trim().isEmpty() ? args[1] : DEFAULT_LOG_FILE : DEFAULT_LOG_FILE;

        List<Integer> periods = args.length > 2
                ? args[2] != null && !args[2].trim().isEmpty()
                ? Arrays.stream(args[2].split(" ")).map(Integer::parseInt).collect(Collectors.toList())
                : DEFAULT_PERIODS
                : DEFAULT_PERIODS;

        int delay = args.length > 3 ? args[3] != null && !args[3].trim().isEmpty() ? Integer.parseInt(args[3]) : DEFAULT_DELAY : DEFAULT_DELAY;

        HeightMonitor heightMonitor = new HeightMonitor(peers, logFile, periods, delay);
        Runtime.getRuntime().addShutdownHook(new Thread(heightMonitor::stop));
        heightMonitor.start();
    }

    private static List<String> readPeers(String arg) {
        try {
            return Files.readAllLines(Paths.get(arg));
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

    private static Logger createLoggerFor(Class<?> clazz, String file) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(file);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setEncoder(ple);
        consoleAppender.setContext(lc);
        consoleAppender.start();

        Logger jettyLogger = lc.getLogger("org.eclipse.jetty");
        jettyLogger.setLevel(Level.OFF);

        Logger logger = (Logger) getLogger(clazz);
        logger.addAppender(fileAppender);
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false); /* set to true if root should log too */

        return logger;
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
