/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.inject.Vetoed;
import lombok.Getter;

/**
 *
 * @author alukin@gmail.com
 */
@Vetoed
public class FileDownloadStatus {
    @Getter
    private final String id;
    @Getter
    double completed = 0.0;
    @Getter
    AtomicInteger chunksTotal = new AtomicInteger(1); //init to 1 to avoid zero division
    @Getter
    AtomicInteger chunksReady = new AtomicInteger(0);
    @Getter    
    FileDownloadDecision decision = FileDownloadDecision.NotReady;
    @Getter    
    Set<PeerFileHashSum> goodPeers;
    @Getter    
    FileDownloadInfo fileDownloadInfo;
    @Getter
    boolean downloaderStarted = false;

    public FileDownloadStatus(String id) {
        this.id = id;
    }

    boolean isComplete() {
        return chunksReady.get() >= chunksTotal.get();
    }
    
    @Override
    public String toString(){
       return String.format("File ID: %s completed %f3.2 decision %s", id, completed,decision.name());
    }
}
