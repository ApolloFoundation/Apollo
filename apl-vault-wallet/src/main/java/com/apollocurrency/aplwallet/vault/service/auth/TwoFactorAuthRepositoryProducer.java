package com.apollocurrency.aplwallet.vault.service.auth;

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

    @Inject
    public TwoFactorAuthRepositoryProducer(DirProvider dirProvider) {
        log.info("The 2FA store is allocated on the file system");
        Path path2FADir = dirProvider.get2FADir();
        this.repositoryFS = new TwoFactorAuthFileSystemRepository(path2FADir);
    }

    @Produces
    @Named("FSRepository")
    public TwoFactorAuthRepository getTwoFactorAuthFSRepository() {
        return repositoryFS;
    }

}