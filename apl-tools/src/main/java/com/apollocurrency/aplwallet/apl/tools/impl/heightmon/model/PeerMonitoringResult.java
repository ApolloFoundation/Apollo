/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.Block;
import com.apollocurrency.aplwallet.apl.util.Version;

import java.util.List;

public class PeerMonitoringResult {
    private List<Block> blocks;
    private Version version;

    public PeerMonitoringResult(List<Block> blocks, Version version) {
        this.blocks = blocks;
        this.version = version;
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
}
