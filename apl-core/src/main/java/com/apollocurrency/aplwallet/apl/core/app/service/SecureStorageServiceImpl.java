/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.service;

import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.model.SecureStorage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
@Singleton
public class SecureStorageServiceImpl implements SecureStorageService {
    private static final Logger LOG = getLogger(SecureStorageServiceImpl.class);

    private Path secureStoragePath;
    private Path secureStoragePathCopy;

    private static final String SECURE_STORE_FILE_NAME = "secure_store";
    private static final String SECURE_STORE_FILE_COPY_NAME = "secure_store_copy";

    private Map<Long, String> store = new ConcurrentHashMap<>();
    private final OptionDAO optionDAO;
    private final PropertyStorageService propertyStorageService;
    private final boolean isEnabled;

    @Inject
    public SecureStorageServiceImpl(@Named("secureStoreDirPath") Path secureStorageDirPath, PropertiesHolder propertiesHolder, OptionDAO optionDAO,
                                    PropertyStorageService propertyStorageService) {
        this.optionDAO = optionDAO;
        this.propertyStorageService = propertyStorageService;

        isEnabled = propertiesHolder.getBooleanProperty("apl.secureStorage.restore.isEnabled");
        log.trace("apl.secureStorage.restore.isEnabled = '{}'", isEnabled);

        if (isEnabled) {
            Objects.requireNonNull(secureStorageDirPath, "secureStorageDirPath can't be null");
            this.secureStoragePath = secureStorageDirPath.resolve(SECURE_STORE_FILE_NAME);
            this.secureStoragePathCopy = secureStorageDirPath.resolve(SECURE_STORE_FILE_COPY_NAME);
            log.trace("secureStoragePath = '{}', secureStoragePathCopy = '{}'", secureStoragePath, secureStoragePathCopy);
            if (!Files.exists(secureStorageDirPath)) {
                try {
                    log.trace("createDirectories, secureStorageDirPath = '{}'", secureStorageDirPath);
                    Files.createDirectories(secureStorageDirPath);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw new RuntimeException(e.toString(), e);
                }
            }

            restoreSecretStorageIfExist();
        }
    }

    @Override
    public void addUserPassPhrase(Long accountId, String passPhrase){
        if (isEnabled) {
            store.put(accountId, passPhrase);

            storeSecretStorage();
        }
    }

    @Override
    public String getUserPassPhrase(Long accountId){
        return store.get(accountId);
    }

    @Override
    public List<Long> getAccounts() {
        return new ArrayList<>(store.keySet());
    }

    /**
     * Store storage.
     * @return true - stored successfully.
     */
    @Override
    public synchronized boolean storeSecretStorage() {
        deleteSecretStorage(secureStoragePathCopy);
        storeSecretStorage(SECURE_STORE_KEY, secureStoragePathCopy);
        log.trace("storeSecretStorage 1, secureStoragePathCopy = '{}'", secureStoragePathCopy);

        // Copy to the original file.
        try {
            Files.copy(secureStoragePathCopy, secureStoragePath, REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return true;
    }


    public boolean storeSecretStorage(String keyName, Path secureStoragePath) {
        log.trace("storeSecretStorage 1, keyName='{}', secureStoragePathCopy = '{}'", keyName, secureStoragePathCopy);
        String privateKey = getPK(keyName);
        SecureStorage secureStore;
        try {
            secureStore = collectAllDataToTempStore();
        } catch (AplException.ExecutiveProcessException e) {
            LOG.error(e.getMessage());
            return false;
        }
        log.trace("storeSecretStorage 2, keyName='{}', secureStoragePath = '{}'", keyName, secureStoragePath);
        return secureStore.store(privateKey, secureStoragePath.toString());
    }

    /**
     * Restore storage.
     * @return true - restored successfully.
     */
    @Override
    public boolean restoreSecretStorage(Path file) {
        String privateKey = getPK(SECURE_STORE_KEY);
        log.trace("restoreSecretStorage, file='{}', privateKey not empty = '{}'", file, privateKey != null);
        if(privateKey == null){
            return false;
        }

        SecureStorage fbWallet = SecureStorage.get(privateKey, file.toString());

        if(fbWallet == null){
            return false;
        }

        fbWallet.get(privateKey, file.toString());
        log.trace("storeAll, file = '{}', keys = [{}]", file.toString(), fbWallet.getDexKeys().size());
        store.putAll(fbWallet.getDexKeys());

        return true;
    }

    /**
     * Delete storage and secret key.
     * @return true deleted successfully.
     */
    @Override
    public boolean deleteSecretStorage(Path path) {
        File file = new File(path.toString());
        log.trace("deleteSecretStorage, path = '{}'", path);
        return file.delete();
    }

    @Override
    public String createPrivateKeyForStorage() {
        byte [] secretBytes = new byte[32];
        Random random = new Random();
        random.nextBytes(secretBytes);
        return Convert.toHexString(secretBytes);
    }


    /**
     * Restore storage if it exist.
     * @return true if file restored.
     */
    public boolean restoreSecretStorageIfExist() {
        File tmpDir = new File(secureStoragePath.toString());
        log.trace("restoreSecretStorageIfExist, tmpDir = '{}'", tmpDir);
        try {
            if (tmpDir.exists()) {
                return restoreSecretStorage(secureStoragePath);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            File oldDir = new File(secureStoragePathCopy.toString());
            if (oldDir.exists()) {
                log.trace("restoreSecretStorageIfExist 2, oldDir = '{}'", oldDir);
                return restoreSecretStorage(secureStoragePathCopy);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return false;
    }

    private SecureStorage collectAllDataToTempStore() throws AplException.ExecutiveProcessException {
        SecureStorage secureStorage = new SecureStorage();
        secureStorage.addDexKeys(store);

        return secureStorage;
    }

    @Override
    public boolean flushAccountKeys(Long accountID, String passPhrase) {
        LOG.debug("flushAccountKeys entry point");
        if (isEnabled && store.containsKey(accountID)) {
            String extractedPass = store.get(accountID);
            if ( extractedPass!=null && extractedPass.equals(passPhrase)) {
                LOG.debug("flushed key for account: {}",accountID);
                store.remove(accountID);

                storeSecretStorage();
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isEnabled() {
        log.trace("isEnabled ? = {}", isEnabled);
        return isEnabled;
    }


    private String getPK(String keyName){
        String privateKey;

        if(propertyStorageService.isExist()){
            Properties properties = propertyStorageService.loadProperties();
            log.trace("getPK, properties = {}", properties);
            privateKey = (String) properties.get(new String(Base64.getDecoder().decode(PropertyStorageService.SS_KEY_NAME)));
            log.trace("getPK, privateKey ? = '{}'", privateKey != null);

            if(privateKey == null){
                String pk = createPrivateKeyForStorage();
                properties.put(new String(Base64.getDecoder().decode(PropertyStorageService.SS_KEY_NAME)), pk);
                log.trace("getPK - storeProperties, properties = {}", properties);
                return propertyStorageService.storeProperties(properties) ? pk : null;
            }
        } else {
            // For users with old version.
            privateKey = optionDAO.get(keyName);
            log.trace("getPK - optionDAO, privateKey ? = {}", privateKey != null);
            if(privateKey == null){
                privateKey = createPrivateKeyForStorage();
                log.trace("getPK - createPrivateKeyForStorage, privateKey ? = {}", privateKey != null);
            }

            String pk = createPrivateKeyForStorage();
            Properties properties = new Properties();
            properties.put(new String(Base64.getDecoder().decode(PropertyStorageService.SS_KEY_NAME)), privateKey);
            log.trace("getPK - createPrivateKeyForStorage, privateKey ? = {}, pk = {}", privateKey != null, pk);

            return propertyStorageService.storeProperties(properties) ? pk : null;
        }

        return privateKey;
    }
}
