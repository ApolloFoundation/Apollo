/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * @author alukin@gmail.com
 */
public class ShardDownloadStatus {
    public final static int NONE = 0;
    public final static int FOUND_IN_PEERS = 1;
    public final static int DNLD_STARTED = 2;
    public final static int OK = 3;
    public final static int FAILED=4;
    //fileId::state
    private final Map<String,Integer> status;
    @Getter
    @Setter
    private boolean sigalFired = false;

    public ShardDownloadStatus(Set<String> fileIds) {
        status = new HashMap<>();
        fileIds.forEach((s) -> {
            status.put(s, NONE);
        });
    }

    public boolean isDownloadCompleted() {
        boolean res = true;
        for (Integer st : status.values()) {
            res = res && (st >= 3);
        }
        return res;
    }

    public boolean isDowloadedOK() {
        boolean res = true;
        for (Integer st : status.values()) {
            res = res && (st == 3);
        }
        return res;
    }

    public void setStatus(String fileId, int fstatus) {
        status.replace(fileId, fstatus);
    }
}
