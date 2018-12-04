/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import java.nio.file.Path;

public interface Downloader {
    Path tryDownload(String uri, byte[] hash);
}
