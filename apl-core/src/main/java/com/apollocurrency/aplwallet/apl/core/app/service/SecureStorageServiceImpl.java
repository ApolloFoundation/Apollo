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

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class SecureStorageServiceImpl implements SecureStorageService {
    private static final Logger LOG = getLogger(SecureStorageServiceImpl.class);

    private Path secureStoragePath;
    private static final String SECURE_STORE_FILE_NAME = "secure_store";
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

            if (!Files.exists(secureStorageDirPath)) {
                try {
                    Files.createDirectories(secureStorageDirPath);
                } catch (IOException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }

            boolean isRestored = restoreSecretStorageIfExist();
            if (isRestored) {
                deleteSecretStorage();
            }
        }
    }

    @Override
    public void addUserPassPhrase(Long accountId, String passPhrase){
        store.put(accountId, passPhrase);
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
    public boolean storeSecretStorage() {
        String privateKey = createPrivateKeyForStorage();

        if(privateKey == null){
            return false;
        }
        optionDAO.set(SECURE_STORE_KEY, privateKey);

        SecureStorage secureStore;
        try {
            secureStore = collectAllDataToTempStore();
        } catch (AplException.ExecutiveProcessException e) {
            LOG.error(e.getMessage());
            return false;
        }
        return secureStore != null && secureStoragePath != null ?  // shutdown case
                secureStore.store(privateKey, secureStoragePath.toString()) : false;
    }

    /**
     * Restore storage.
     * @return true - restored successfully.
     */
    @Override
    public boolean restoreSecretStorage() {
        String privateKey = optionDAO.get(SECURE_STORE_KEY);
        if(privateKey == null){
            return false;
        }

        SecureStorage fbWallet = SecureStorage.get(privateKey, secureStoragePath.toString());

        if(fbWallet == null){
            return false;
        }

        fbWallet.get(privateKey, secureStoragePath.toString());

        store.putAll(fbWallet.getDexKeys());

        return true;
    }

    /**
     * Delete storage and secret key.
     * @return true deleted successfully.
     */
    @Override
    public boolean deleteSecretStorage() {
        File file = new File(secureStoragePath.toString());
        optionDAO.delete(SECURE_STORE_KEY);

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
        if (tmpDir.exists()){
            return restoreSecretStorage();
        }
        return false;
    }

    private SecureStorage collectAllDataToTempStore() throws AplException.ExecutiveProcessException {
        SecureStorage secureStorage = new SecureStorage();
        secureStorage.addDexKeys(store);

        return secureStorage;
    }
}
