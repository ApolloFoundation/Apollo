package com.apollocurrency.aplwallet.apl.security;

import com.apollocurrency.apl.id.cert.CertKeyPersistence;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.apl.id.cert.ExtCert;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


/**
 * Temporal class to load certs from filesystem for {@link com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateV2TransactionType} transaction
 * Should be removed, when p2p message exchange protocol will be implemented
 * Moved from UpdaterV2 impl
 */
@Slf4j
public class CertificateLoader {
    private final Class<?> loadClass;
    private final Version version;

    public CertificateLoader(Class<?> tClass, Version appVersion) {
        this.loadClass = tClass;
        this.version = appVersion;
    }

    public List<ExtCert> loadAll() throws IOException {
        Path rootPath = Paths.get(loadClass.getProtectionDomain().getCodeSource().getLocation().getPath());
        List<ExtCert> certs = new ArrayList<>();
        Path fsPath = null;
        if (rootPath.toUri().getScheme().equals("jar")) {
            fsPath = rootPath;
        } else if (Files.isRegularFile(rootPath)) {
            String certsZipPath = "certs" + "-" + version + ".zip";
            fsPath = rootPath.getParent().resolve(certsZipPath);
        }
        if (fsPath != null) {
            certs.addAll(readAllFromFs(fsPath));
        } else if (Files.isDirectory(rootPath)) { //classes
            certs.addAll(readAll(rootPath));
        } else {
            throw new RuntimeException("Unable to read certs from " + rootPath.toAbsolutePath());
        }
        log.info("path {}, was read {} certs", rootPath.toAbsolutePath(), certs.size());
        return certs;
    }

    private List<ExtCert> readAll(Path path) throws IOException {
        List<ExtCert> certs = new ArrayList<>();
        Files.walkFileTree(path.resolve("certs"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    ExtCert cert = CertKeyPersistence.loadPEMFromPath(file.toAbsolutePath());
                    certs.add(cert);
                }
                catch (Exception e) {
                    log.error("Unable to load certificate from " + file, e);
                }
                return super.visitFile(file, attrs);
            }
        });
        return certs;
    }

    private List<ExtCert> readAllFromFs(Path path) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader())) {
            return readAll(fileSystem.getPath("/"));
        }
    }
}