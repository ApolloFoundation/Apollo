/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import com.apollocurrency.aplwallet.apl.udpater.intfce.DownloadInfo;

import java.nio.file.Path;

public interface Downloader {
    Path tryDownload(String uri, byte[] hash);

    DownloadInfo getDownloadInfo();
}
