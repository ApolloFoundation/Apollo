/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.inject.Vetoed;

/**
 *
 * @author alukin@gmail.com
 */
@Vetoed
public class FileDownloadStatus {
    
    double completed = 0.0;
    AtomicInteger chunksTotal = new AtomicInteger(1); //init to 1 to avoid zero division
    AtomicInteger chunksReady = new AtomicInteger(0);
    List<String> peers = new ArrayList<>();
    FileDownloadDecision decision = FileDownloadDecision.NotReady;

    boolean isComplete() {
        return chunksReady.get() >= chunksTotal.get();
    }
    
}
