/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import dto.Block;
import org.slf4j.Logger;

public class HeightMonitor {
    private static final Logger LOG = getLogger(HeightMonitor.class);
    public static void main(String[] args) {
        NodeClient client = new NodeClient();
        List<String> peers = Arrays.asList(
                "51.15.235.41",
                "163.172.146.173",
                "51.15.69.39",
                "51.15.59.37",
                "51.15.114.68"
        );
        List<String> peersUrls = peers.stream().map(peer -> "http://" + peer + ":6876/apl").collect(Collectors.toList());

        int allTimeMax = -1;
        int last3HoursMax = -1;
        int last6HoursMax = -1;
        int last12HoursMax = -1;
        int last24HoursMax = -1;
        long startTime = System.currentTimeMillis();
        try (FileWriter writer = new FileWriter("heightMonitorLogs")) {
            while (true) {

                Map<String, List<Block>> peerBlocks = new HashMap<>();
                for (int i = 0; i < peersUrls.size(); i++) {
                    try {
                        List<Block> blocksList = client.getBlocksList(peersUrls.get(i), false, null);
                        peerBlocks.put(peers.get(i), blocksList);

                    }
                    catch (Throwable e) {
                        writer.append(e.getMessage()).append(" Cannot connect to peer: ").append(peers.get(i)).append("\n");
                        peerBlocks.put(peers.get(i), new ArrayList<>());
                    }
                }
                for (int i = 0; i < peers.size(); i++) {
                    List<Block> targetBlocks = peerBlocks.get(peers.get(i));
                    for (int j = i + 1; j < peers.size(); j++) {
                        List<Block> blocksToCompare = peerBlocks.get(peers.get(j));
                        Block lastMutualBlock = findLastMutualBlock(blocksToCompare, targetBlocks);
                        if (lastMutualBlock == null) {
                            writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - ERROR! No mutual blocks between ").append(peers.get(i)).append(" and ").append(peers.get(j)).append(
                                    "\n");
                            continue;
                        }
                        int lastHeight = targetBlocks.get(0).getHeight();
                        int mutualBlockHeight = lastMutualBlock.getHeight();
                        int blocksDiff = lastHeight - mutualBlockHeight;
                        allTimeMax = Math.max(blocksDiff, allTimeMax);
                        last3HoursMax = Math.max(blocksDiff, last3HoursMax);
                        last6HoursMax = Math.max(blocksDiff, last6HoursMax);
                        last12HoursMax = Math.max(blocksDiff, last12HoursMax);
                        last24HoursMax = Math.max(blocksDiff, last24HoursMax);
                        writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - Blocks diff is ").append(String.valueOf(blocksDiff)).append(" between peers ").append(peers.get(i)).append(" and ").append(peers.get(j)).append("\n");
                        writer.flush();
                    }
                }
                writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - MAX Blocks diff is ").append(String.valueOf(allTimeMax)).append("\n");
                writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - MAX Blocks diff for last 3h is  ").append(String.valueOf(last3HoursMax)).append("\n");
                writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - MAX Blocks diff for last 6h is  ").append(String.valueOf(last6HoursMax)).append("\n");
                writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - MAX Blocks diff for last 12h is  ").append(String.valueOf(last12HoursMax)).append("\n");
                writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - MAX Blocks diff for last 24h is  ").append(String.valueOf(last24HoursMax)).append("\n");
                long currentTime = System.currentTimeMillis();
                if (((currentTime - startTime) / (1000 * 60 * 60)) % 3 == 0) {
                    last3HoursMax = 0;
                }
                if (((currentTime - startTime) / (1000 * 60 * 60)) % 6 == 0) {
                    last6HoursMax = 0;
                }
                if (((currentTime - startTime) / (1000 * 60 * 60)) % 12 == 0) {
                    last12HoursMax = 0;
                }
                if (((currentTime - startTime) / (1000 * 60 * 60)) % 24 == 0) {
                    last24HoursMax = 0;
                }
                TimeUnit.SECONDS.sleep(30);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Block findLastMutualBlock(List<Block> blocksToCompare, List<Block> targetBlocks) {
        for (int i = 0; i < blocksToCompare.size(); i++) {
            Block block = blocksToCompare.get(i);
            if (targetBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }
}
