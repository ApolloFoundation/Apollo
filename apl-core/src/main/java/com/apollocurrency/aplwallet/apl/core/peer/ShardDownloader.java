/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import javax.inject.Inject;

/**
 *
 * @author alukin@gmailk.com
 */
public class ShardDownloader {
    
    private final FileDownloader fileDownloader;

    @Inject
    public ShardDownloader(FileDownloader fileDownloader) {
        this.fileDownloader = fileDownloader;
    }
    
}
