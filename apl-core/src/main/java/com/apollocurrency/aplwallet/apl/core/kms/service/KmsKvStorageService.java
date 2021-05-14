package com.apollocurrency.aplwallet.apl.core.kms.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.core.kms.config.DatabaseKVStorageConfigParametersImpl;
import io.firstbridge.kms.persistence.storage.KVStorageProviderFactory;
import io.firstbridge.kms.persistence.storage.repository.KmsAccountRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Service cdi component for Kms KV storage access
 */
@Slf4j
@Singleton
public class KmsKvStorageService {

    private KmsAccountRepository kmsStorage;
    private KVStorageProviderFactory kvStorageProviderFactory;

    @Inject
    public KmsKvStorageService(DbConfig dbConfig) {
        Objects.requireNonNull(dbConfig, "dbConfig is NULL");
        Objects.requireNonNull(dbConfig.getDbProperties(), "Db Properties is NULL");

        if (dbConfig.getKmsSchemaName().isPresent()) {
            this.kvStorageProviderFactory = new KVStorageProviderFactory(new DatabaseKVStorageConfigParametersImpl(dbConfig));
            this.kmsStorage = this.kvStorageProviderFactory.getKmsAccountRepository();
        } else {
            // TODO: YL rest client should be used for access remote REST API
        }
    }

    public KmsAccountRepository getKmsStorage() {
        return this.kmsStorage;
    }

    public void shutdown() {
        if (this.kvStorageProviderFactory != null) {
            this.kvStorageProviderFactory.shutdown();
        }
    }
}
