/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TwoFactorAuthFileSystemRepository implements TwoFactorAuthRepository {
    private static final String DEFAULT_SUFFIX = ".copy";
    private final String suffix;
    private final Path twoFactorDirPath;

    public TwoFactorAuthFileSystemRepository(Path directory, String suffix) {
        Objects.requireNonNull(suffix);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
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
        Path path = findFile(Convert2.defaultRsAccount(account));
        if (path == null) {
            return null;
        }
        try {
            return JSON.getMapper().readValue(path.toFile(), TwoFactorAuthEntity.class);
        } catch (IOException e) {
            log.warn("Read 2fa json error", e);
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
        log.trace("Add new 2fa-file = {}", entity2FA.getAccount());
        Path path = getNewFilePath(Convert2.defaultRsAccount(entity2FA.getAccount()));
        if (path == null) {
            return false;
        }
        try {
            JSON.writeJson(path, entity2FA);
            return Files.exists(path);
        } catch (IOException e) {
            log.debug("Write 2fa json error", e);
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
        String rsAccount = Convert2.defaultRsAccount(entity2FA.getAccount());
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
        } catch (IOException e) {
            log.debug("2fa update json error", e);
            try {
                Files.deleteIfExists(copyPath);
            } catch (IOException e1) {
                log.debug("Delete 2fa copied file error", e1);
            }
        }
        return false;
    }

    @Override
    public boolean delete(long account) {
        log.trace("delete 2fa by account = {}", account);
        TwoFactorAuthEntity entity = get(account);
        if (entity != null && entity.getAccount() == account) {
            try {
                Files.delete(twoFactorDirPath.resolve(Convert2.defaultRsAccount(account)));
                return get(account) == null;
            } catch (IOException e) {
                log.debug("2fa delete json error", e);
            }
        }
        return false;
    }

    @Override
    public List<TwoFactorAuthEntity> selectAll() {
        // should not be called on 2fa file repository
        throw new UnsupportedOperationException("selecting all records for 2fa repo is not suported now...");
    }
}
