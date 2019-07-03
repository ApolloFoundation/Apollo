/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.Block;
import com.apollocurrency.aplwallet.apl.util.Version;

import java.util.List;

public class PeerMonitoringResult {
    private List<Block> blocks;
    private List<ShardDTO> shards;
    private int height;
    private Version version;

    public PeerMonitoringResult(List<Block> blocks, Version version, int height, List<ShardDTO> shards) {
        this.blocks = blocks;
        this.height = height;
        this.version = version;
        this.shards = shards;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public List<ShardDTO> getShards() {
        return shards;
    }

    public void setShards(List<ShardDTO> shards) {
        this.shards = shards;
    }
}
