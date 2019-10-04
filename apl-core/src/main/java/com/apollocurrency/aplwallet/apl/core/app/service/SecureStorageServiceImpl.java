package com.apollocurrency.aplwallet.apl.core.app.service;

import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.model.SecureStorage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class SecureStorageServiceImpl implements SecureStorageService {
    private static final Logger LOG = getLogger(SecureStorageServiceImpl.class);

    private Path secureStoragePath;
    private Path secureStoragePathCopy;

    private static final String SECURE_STORE_FILE_NAME = "secure_store";
    private static final String SECURE_STORE_FILE_COPY_NAME = "secure_store_copy";
    private static final String SECURE_STORE_KEY = "secure_store_key";

    private Map<Long, String> store = new ConcurrentHashMap<>();
    private OptionDAO optionDAO;

    @Inject
    public SecureStorageServiceImpl(@Named("secureStoreDirPath") Path secureStorageDirPath, PropertiesHolder propertiesHolder, OptionDAO optionDAO) {
        this.optionDAO = optionDAO;

        boolean restore = propertiesHolder.getBooleanProperty("apl.secureStorage.restore.isEnable");

        if(restore) {
            Objects.requireNonNull(secureStorageDirPath, "secureStorageDirPath can't be null");
            this.secureStoragePath = secureStorageDirPath.resolve(SECURE_STORE_FILE_NAME);
            this.secureStoragePathCopy = secureStorageDirPath.resolve(SECURE_STORE_FILE_COPY_NAME);

            if (!Files.exists(secureStorageDirPath)) {
                try {
                    Files.createDirectories(secureStorageDirPath);
                } catch (IOException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }

            restoreSecretStorageIfExist();
        }
    }

    @Override
    public void addUserPassPhrase(Long accountId, String passPhrase){
        store.put(accountId, passPhrase);

        storeSecretStorage();
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

        // Copy to the original file.
        try {
            Files.copy(secureStoragePathCopy, secureStoragePath, REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return true;
    }


    public boolean storeSecretStorage(String keyName, Path secureStoragePath) {
        String privateKey = optionDAO.get(keyName);

        if(privateKey == null){
            optionDAO.set(keyName, createPrivateKeyForStorage());
        }
        privateKey = optionDAO.get(keyName);

        SecureStorage secureStore;
        try {
            secureStore = collectAllDataToTempStore();
        } catch (AplException.ExecutiveProcessException e) {
            LOG.error(e.getMessage());
            return false;
        }

        return secureStore.store(privateKey, secureStoragePath.toString());
    }

    /**
     * Restore storage.
     * @return true - restored successfully.
     */
    @Override
    public boolean restoreSecretStorage(Path file) {
        String privateKey = optionDAO.get(SECURE_STORE_KEY);
        if(privateKey == null){
            return false;
        }

        SecureStorage fbWallet = SecureStorage.get(privateKey, file.toString());

        if(fbWallet == null){
            return false;
        }

        fbWallet.get(privateKey, file.toString());

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
        return file.delete();
    }

    @Override
    public String createPrivateKeyForStorage() {
        byte [] secretBytes = new byte[32];
        Random random = new Random();
        random.nextBytes(secretBytes);
        String privateKey = Convert.toHexString(secretBytes);

        return privateKey;
    }


    /**
     * Restore storage if it exist.
     * @return true if file restored.
     */
    public boolean restoreSecretStorageIfExist() {
        File tmpDir = new File(secureStoragePath.toString());
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
        if (store.containsKey(accountID)) {            
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
}
