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

/**
 * The 2FA Repository producer
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class TwoFactorAuthRepositoryProducer {

    private final TwoFactorAuthRepository repository;

    @Inject
    public TwoFactorAuthRepositoryProducer(DatabaseManager databaseManager,
                                           DirProvider dirProvider,
                                           @Property("apl.store2FAInFileSystem") boolean isStorInFileSystem,
                                           @Property("apl.issuerSuffix2FA") String issuerSuffix2FA) {

        log.info("The 2FA store is allocated {}.", isStorInFileSystem? "on the file system" : "in the data base");
        this.repository = isStorInFileSystem ?
            new TwoFactorAuthFileSystemRepository(dirProvider.get2FADir()):
            new TwoFactorAuthRepositoryImpl(databaseManager.getDataSource());
    }

    @Produces
    public TwoFactorAuthRepository getTwoFactorAuthRepository(){
        return repository;
    }

}
