/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.service;

import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.updater.*;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSADoubleDecryptor;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.updater.downloader.DownloaderImpl;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class UpdaterServiceImpl implements UpdaterService {
    private static final UrlExtractor defaultUrlExtractor = new SimpleUrlExtractor(new RSADoubleDecryptor());
    private static final Downloader defaultDownloader = new DownloaderImpl(new DownloadInfo(), UpdaterConstants.NEXT_ATTEMPT_TIMEOUT,
            UpdaterConstants.DOWNLOAD_ATTEMPTS, new SHA256ConsistencyVerifier());
    private UrlExtractor urlExtractor;
    private SecurityAlertSender securityAlertSender;
    private Unpacker unpacker;
    private AuthorityChecker authorityChecker;
    private Downloader downloader;
    private UpdaterRepository updaterRepository;

    public UpdaterServiceImpl(SecurityAlertSender securityAlertSender, Unpacker unpacker, AuthorityChecker authorityChecker, Downloader downloader, UrlExtractor urlExtractor,
                              UpdaterRepository updaterRepository) {
        this.securityAlertSender = securityAlertSender;
        this.unpacker = unpacker;
        this.authorityChecker = authorityChecker;
        this.downloader = downloader;
        this.urlExtractor = urlExtractor;
        this.updaterRepository = updaterRepository;
    }

    public UpdaterServiceImpl(Downloader downloader, UpdaterRepository repository) {
        this(new ConsoleSecurityAlertSender(), new JarUnpacker(""), new AuthorityCheckerImpl(UpdaterConstants.CA_CERTIFICATE_URL), downloader, defaultUrlExtractor, repository);
    }

    public UpdaterServiceImpl(UpdaterRepository repository) {
        this(defaultDownloader, repository);
    }

    @Override
    public Path unpack(Path file) throws IOException {
        return unpacker.unpack(file);
    }

    @Override
    public boolean verifyCertificates(String certificateDirectory) {
        return authorityChecker.verifyCertificates(certificateDirectory);
    }

    @Override
    public boolean verifyJarSignature(String certificateDirectory, Path jarFilePath) {
        return
        authorityChecker.verifyJarSignature(certificateDirectory, jarFilePath);
    }

    @Override
    public Path tryDownload(String uri, byte[] hash) {
        return downloader.tryDownload(uri, hash);
    }

    @Override
    public void sendSecurityAlert(Transaction invalidUpdateTransaction) {
        securityAlertSender.send(invalidUpdateTransaction);
    }

    @Override
    public void sendSecurityAlert(String message) {
        securityAlertSender.send(message);
    }


    @Override
    public String extractUrl(byte[] encryptedUrlBytes, Pattern urlPattern) {
        return urlExtractor.extract(encryptedUrlBytes, urlPattern);
    }

    public UpdateTransaction getLast() {return updaterRepository.getLast();}

    public void save(UpdateTransaction transaction) {updaterRepository.save(transaction);}

    public void update(UpdateTransaction transaction) {updaterRepository.update(transaction);}

    public int clear() {return updaterRepository.clear();}

    public void clearAndSave(UpdateTransaction transaction) {updaterRepository.clearAndSave(transaction);}
}
