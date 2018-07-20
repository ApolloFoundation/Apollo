package com.apollocurrency.aplwallet.apl.updater.downloader;

import java.io.IOException;
import java.nio.file.Path;

public interface DownloadExecutor {
    Path download(String uri) throws IOException;
}
