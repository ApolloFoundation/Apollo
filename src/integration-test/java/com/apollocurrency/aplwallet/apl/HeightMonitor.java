/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dto.Block;
import org.slf4j.Logger;

public class HeightMonitor {
    private static final Logger LOG = getLogger(HeightMonitor.class);
    public static void main(String[] args) {
        NodeClient client = new NodeClient();
        List<String> peers = Arrays.asList(
                "http://51.15.235.41:6876/apl",
                "http://163.172.146.173:6876/apl",
                "http://51.15.69.39:6876/apl",
                "http://51.15.59.37:6876/apl",
                "http://51.15.114.68:6876/apl"
        );
        try (FileWriter writer = new FileWriter("heightMonitorLogs")) {
            start:
            while (true) {
                Map<String, List<Block>> peerBlocks = new HashMap<>();
                for (int i = 0; i < peers.size(); i++) {
                    try {
                        List<Block> blocksList = client.getBlocksList(peers.get(i), false, null);
                        peerBlocks.put(peers.get(i), blocksList);

                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                        continue start;
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
                        writer.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append(" - Blocks diff is ").append(String.valueOf(lastHeight - mutualBlockHeight)).append("between peers ").append(peers.get(i)).append(" and ").append(peers.get(j)).append("\n");
                        writer.flush();
                    }
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
