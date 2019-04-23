/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.HeightMonitorConfig;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.NetworkStats;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.PeerInfo;

import java.net.UnknownHostException;
import java.util.List;

public interface HeightMonitorService {
    NetworkStats getLastStats();

    NetworkStats updateStats();

    void setUp(HeightMonitorConfig config);

    boolean addPeer(PeerInfo peerInfo) throws UnknownHostException;

    List<PeerInfo> getAllPeers();
}
