/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.Block;
import com.apollocurrency.aplwallet.apl.util.Version;

import java.util.List;
import java.util.Map;

public class PeerMonitoringResult {
    private List<ShardDTO> shards;
    private int height;
    private Version version;
    private Map<String, Block> peerMutualBlocks;

    public PeerMonitoringResult(List<ShardDTO> shards, int height, Version version, Map<String, Block> peerMutualBlocks) {
        this.shards = shards;
        this.height = height;
        this.version = version;
        this.peerMutualBlocks = peerMutualBlocks;
    }

    public Map<String, Block> getPeerMutualBlocks() {
        return peerMutualBlocks;
    }

    public void setPeerMutualBlocks(Map<String, Block> peerMutualBlocks) {
        this.peerMutualBlocks = peerMutualBlocks;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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
