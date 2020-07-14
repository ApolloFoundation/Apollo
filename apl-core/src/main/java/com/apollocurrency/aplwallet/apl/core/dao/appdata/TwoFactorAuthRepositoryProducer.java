/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
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

    private final TwoFactorAuthRepository repositoryFS;
    private final TwoFactorAuthRepository repositoryDB;

    @Inject
    public TwoFactorAuthRepositoryProducer(DatabaseManager databaseManager,
                                           DirProvider dirProvider) {

        log.info("The 2FA store is allocated on the file system");
        Path path2FADir = dirProvider.get2FADir();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        this.repositoryFS = new TwoFactorAuthFileSystemRepository(path2FADir);
        this.repositoryDB = new TwoFactorAuthRepositoryImpl(dataSource);
    }

    @Produces
    @Named("FSRepository")
    public TwoFactorAuthRepository getTwoFactorAuthFSRepository() {
        return repositoryFS;
    }

    @Produces
    @Named("DBRepository")
    public TwoFactorAuthRepository getTwoFactorAuthDBRepository() {
        return repositoryDB;
    }

}
