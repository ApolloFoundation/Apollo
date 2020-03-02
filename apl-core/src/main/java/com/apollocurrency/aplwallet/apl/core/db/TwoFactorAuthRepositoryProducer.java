/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;

/**
 * The 2FA Repository producer
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class TwoFactorAuthRepositoryProducer {

    private final TwoFactorAuthRepository repository;
    private final Path path2FADir;
    private final TransactionalDataSource dataSource;

    @Inject
    public TwoFactorAuthRepositoryProducer(DatabaseManager databaseManager,
                                           DirProvider dirProvider,
                                           @Property("apl.store2FAInFileSystem") boolean isStoreInFileSystem,
                                           @Property("apl.issuerSuffix2FA") String issuerSuffix2FA) {

        log.info("The 2FA store is allocated {}.", isStoreInFileSystem? "on the file system" : "in the data base");
        path2FADir = dirProvider.get2FADir();
        dataSource = databaseManager.getDataSource();

        this.repository = isStoreInFileSystem ?
            new TwoFactorAuthFileSystemRepository(path2FADir):
            new TwoFactorAuthRepositoryImpl(dataSource);
    }

    @Produces
    public TwoFactorAuthRepository getTwoFactorAuthRepository(){
        return repository;
    }

}
