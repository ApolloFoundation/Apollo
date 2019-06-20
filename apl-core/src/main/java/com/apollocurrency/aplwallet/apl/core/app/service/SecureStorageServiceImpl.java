package com.apollocurrency.aplwallet.apl.core.app.service;

import com.apollocurrency.aplwallet.apl.core.model.SecureStorage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class SecureStorageServiceImpl implements SecureStorageService {
    private static final Logger LOG = getLogger(SecureStorageServiceImpl.class);

    private Path secureStoragePath;
    private static final String SECURE_STORE_FILE_NAME = "secure_store";
    private String privateKey;
    private Map<Long, String> store = new ConcurrentHashMap<>();

    @Inject
    public SecureStorageServiceImpl(@Named("secureStoreDirPath") Path secureStorageDirPath) {
        Objects.requireNonNull(secureStorageDirPath, "secureStorageDirPath can't be null");

        this.secureStoragePath = secureStorageDirPath.resolve(SECURE_STORE_FILE_NAME);
        this.privateKey = createPrivateKeyForStorage();

        if (!Files.exists(secureStorageDirPath)) {
            try {
                Files.createDirectories(secureStorageDirPath);
            }
            catch (IOException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        boolean isRestored = restoreSecretStorageIfExist();

        if(isRestored){
            deleteSecretStorage();
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
    public boolean storeSecretStorage() {
        SecureStorage secureStore;
        try {
            secureStore = collectAllDataToTempStore();
        } catch (AplException.ExecutiveProcessException e) {
            LOG.error(e.getMessage());
            return false;
        }
        return secureStore.store(privateKey, secureStoragePath.toString());
    }

    @Override
    public boolean restoreSecretStorage() {
        Objects.requireNonNull(privateKey);

        SecureStorage fbWallet = SecureStorage.get(privateKey, secureStoragePath.toString());
        fbWallet.get(privateKey, secureStoragePath.toString());

        store.putAll(fbWallet.getDexKeys());

        return true;
    }

    @Override
    public boolean deleteSecretStorage() {
        File file = new File(secureStoragePath.toString());
        return file.delete();
    }

    @Override
    public String createPrivateKeyForStorage(){
        String key;
        Enumeration<NetworkInterface> macs;
        try {
            macs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
        key = Collections.list(macs).stream()
                .filter(m -> {
                    try {
                        return m.getHardwareAddress() != null;
                    } catch (SocketException e) {
                        return false;
                    }
                })
                .map(m-> {
                    try {
                        return Convert.toHexString(m.getHardwareAddress());
                    } catch (SocketException e) {
                       return null;
                    }
                })
                .filter(StringUtils::isNotBlank)
                .sorted()
                .collect(Collectors.joining(  ));
        return key;
    }


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
