/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.slf4j.Logger;

public class TwoFactorAuthFileSystemRepository implements TwoFactorAuthRepository {
    private static final Logger LOG = getLogger(TwoFactorAuthFileSystemRepository.class);
    private static final String DEFAULT_SUFFIX = ".copy";
    private final String suffix;
    private Path twoFactorDirPath;

    public TwoFactorAuthFileSystemRepository(Path directory, String suffix) {
        Objects.requireNonNull(suffix);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            }
            catch (IOException e) {
                throw new RuntimeException(e.toString(), e);
            }
        } else if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("2fa dir path should be directory");
        }
        this.twoFactorDirPath = directory;
        this.suffix = suffix;
    }

    public TwoFactorAuthFileSystemRepository(Path directory) {
        this(directory, DEFAULT_SUFFIX);
    }
    @Override
    public TwoFactorAuthEntity get(long account) {
        Path path = findFile(Convert.defaultRsAccount(account));
        if (path == null) {
            return null;
        }
        try {
            return JSON.getMapper().readValue(path.toFile(), TwoFactorAuthEntity.class);
        }
        catch (IOException e) {
            LOG.debug("Read 2fa json error", e);
            return null;
        }
    }

    private Path findFile(String name) {
        Path targetPath = twoFactorDirPath.resolve(name);
        if (!Files.exists(targetPath)) {
            return null;
        }
        return targetPath;
    }


    @Override
    public boolean add(TwoFactorAuthEntity entity2FA) {
        Path path = getNewFilePath(Convert.defaultRsAccount(entity2FA.getAccount()));
        if (path == null) {
            return false;
        }
        try {
            JSON.writeJson(path, entity2FA);
            return Files.exists(path);
        }
        catch (IOException e) {
            LOG.debug("Write 2fa json error", e);
            return false;
        }
    }

    private Path getNewFilePath(String name) {
        Path targetPath = twoFactorDirPath.resolve(name);
        if (Files.exists(targetPath)) {
            return null;
        }
        return targetPath;
    }

    @Override
    public boolean update(TwoFactorAuthEntity entity2FA) {
        TwoFactorAuthEntity entity = get(entity2FA.getAccount());
        if (entity == null) {
            return false;
        }
        String rsAccount = Convert.defaultRsAccount(entity2FA.getAccount());
        Path targetPath = twoFactorDirPath.resolve(rsAccount);
        Path copyPath = Paths.get(targetPath.toString() + suffix);

        try {
            JSON.writeJson(copyPath, entity2FA);
            Path movedTargetPath = Files.move(copyPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            if (!movedTargetPath.getFileName().toString().equals(rsAccount)) {
                return false;
            }
            TwoFactorAuthEntity savedEntity = get(entity2FA.getAccount());
            return savedEntity.equals(entity2FA);
        }
        catch (IOException e) {
            LOG.debug("2fa update json error", e);
            try {
                Files.deleteIfExists(copyPath);
            }
            catch (IOException e1) {
                LOG.debug("Delete 2fa copied file error", e1);
            }
        }
        return false;
    }

    @Override
    public boolean delete(long account) {
        TwoFactorAuthEntity entity = get(account);
        if (entity != null && entity.getAccount() == account) {
            try {
                Files.delete(twoFactorDirPath.resolve(Convert.defaultRsAccount(account)));
                return get(account) == null;
            }
            catch (IOException e) {
                LOG.debug("2fa delete json error", e);
            }
        }
        return false;
    }
}
