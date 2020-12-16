/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

/**
 * Perform all application data migration
 */
public class ApplicationDataMigrationManager {

/*    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDataMigrationManager.class);

    @Inject
    private VaultKeystoreMigrationExecutor vaultKeystoreMigrationExecutor;
    @Inject
    private DbMigrationExecutor dbMigrationExecutor;
    @Inject
    private TwoFactorAuthMigrationExecutor twoFactorAuthMigrationExecutor;
    @Inject
    private PublicKeyMigrator publicKeyMigrator;
    @Inject
    private ReferencedTransactionMigrator referencedTransactionMigrator;
    @Inject
    private TransactionPublicKeyMigrator transactionPublicKeyMigrator;
    @Inject
    private DirProvider dirProvider;

    public void executeDataMigration() {
        try {
//            String customDbDir = propertiesHolder.getStringProperty("apl.customDbDir");
            String fileName = Constants.APPLICATION_DIR_NAME;
//            if (!StringUtils.isBlank(customDbDir)) {
//                fileName = propertiesHolder.getStringProperty("apl.dbName");
//            }
            Path targetDbPath = dirProvider.getDbDir().resolve(fileName);
            dbMigrationExecutor.performMigration(targetDbPath);
            Path target2FADir = dirProvider.get2FADir();
            twoFactorAuthMigrationExecutor.performMigration(target2FADir);
            Path targetKeystoreDir = dirProvider.getVaultKeystoreDir();
            vaultKeystoreMigrationExecutor.performMigration(targetKeystoreDir);

            if (!dbMigrationExecutor.isAutoCleanup()) {
                dbMigrationExecutor.performAfterMigrationCleanup(targetDbPath);
            }
            if (!vaultKeystoreMigrationExecutor.isAutoCleanup()) {
                vaultKeystoreMigrationExecutor.performAfterMigrationCleanup(targetKeystoreDir);
            }
            if (!twoFactorAuthMigrationExecutor.isAutoCleanup()) {
                twoFactorAuthMigrationExecutor.performAfterMigrationCleanup(target2FADir);
            }
//            publicKeyMigrator.migrate(); // commented out because node fails here after first restart
            referencedTransactionMigrator.migrate();
            transactionPublicKeyMigrator.migrate();
        } catch (IOException e) {
            LOG.error("Fatal error. Cannot proceed data migration", e);
            System.exit(-1);
        }
    }

    public void setVaultKeystoreMigrationExecutor(VaultKeystoreMigrationExecutor vaultKeystoreMigrationExecutor) {
        this.vaultKeystoreMigrationExecutor = vaultKeystoreMigrationExecutor;
    }

    public void setDbMigrationExecutor(DbMigrationExecutor dbMigrationExecutor) {
        this.dbMigrationExecutor = dbMigrationExecutor;
    }

    public void setTwoFactorAuthMigrationExecutor(TwoFactorAuthMigrationExecutor twoFactorAuthMigrationExecutor) {
        this.twoFactorAuthMigrationExecutor = twoFactorAuthMigrationExecutor;
    }

    public void setPublicKeyMigrator(PublicKeyMigrator publicKeyMigrator) {
        this.publicKeyMigrator = publicKeyMigrator;
    }*/
}
