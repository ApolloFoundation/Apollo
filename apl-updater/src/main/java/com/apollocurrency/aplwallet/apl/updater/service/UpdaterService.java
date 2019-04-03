/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.service;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.udpater.intfce.DownloadInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public interface UpdaterService {
    String extractUrl(byte[] encryptedUrlBytes, Pattern urlPattern);
    DownloadInfo getDownloadInfo();
    Path unpack(Path file) throws IOException;

    boolean verifyCertificates(String certificateDirectory);

    boolean verifyJarSignature(String certificateDirectory, Path jarFilePath);

    Path tryDownload(String uri, byte[] hash);

    void sendSecurityAlert(Transaction invalidUpdateTransaction);

    void sendSecurityAlert(String message);

    UpdateTransaction getLast();

    void save(UpdateTransaction transaction);

    void update(UpdateTransaction transaction);

    int clear();

    void clearAndSave(UpdateTransaction transaction);

}
