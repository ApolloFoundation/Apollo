/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.updater.DownloadInfo;

public interface Downloader {
    Path tryDownload(String uri, byte[] hash);

    DownloadInfo getDownloadInfo();
}
