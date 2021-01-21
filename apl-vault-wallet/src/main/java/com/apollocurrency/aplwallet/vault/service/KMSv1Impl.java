package com.apollocurrency.aplwallet.vault.service;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.UserKeyStore;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.util.FbWalletUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class KMSv1Impl implements KMSv1 {

    private final KeyStoreService keyStoreService;

    @Inject
    public KMSv1Impl(KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }


    @Override
    public boolean isWalletExist(long accountId) {
        return keyStoreService.isKeyStoreForAccountExist(accountId);
    }

    @Override
    public boolean isEthKeyExist(long accountId, String passphrase, String ethWalletAddress) {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWallet = walletKeysInfo.getEthWalletForAddress(ethWalletAddress);
        return ethWallet != null;
    }

    @Override
    public WalletKeysInfo getWalletInfo(long accountId, String passphrase) {
        return keyStoreService.getWalletKeysInfo(passphrase, accountId);
    }

    @Override
    public byte[] getAplSecretBytes(long accountId, String passphrase) {
        ApolloFbWallet fbWallet = keyStoreService.getSecretStore(passphrase, accountId);
        return fbWallet != null ? Convert.parseHexString(fbWallet.getAplKeySecret()) : null;
    }

    @Override
    public KMSResponseStatus storeWallet(byte[] wallet, String passPhrase) {
        ApolloFbWallet apolloFbWallet = FbWalletUtil.buildWallet(wallet, passPhrase);

        if (apolloFbWallet == null) {
            return KMSResponseStatus.BAD_CREDENTIALS;
        }

        return keyStoreService.saveSecretKeyStore(passPhrase, apolloFbWallet);
    }


    @Override
    public UserKeyStore exportUserKeyStore(long accountId, String passphrase) throws RestParameterException {
        File keyStore = keyStoreService.getSecretStoreFile(accountId, passphrase);
        if (keyStore == null) {
            return null;
        }
        try {
            return new UserKeyStore(Files.readAllBytes(keyStore.toPath()), keyStore.getName());
        } catch (IOException e) {
            throw new RestParameterException(ApiErrors.EXPORT_KEY_READ_WALLET);
        }
    }

    @Override
    public EthWalletKey getEthWallet(long accountId, String passphrase, String ethAddress) throws RestParameterException {
        WalletKeysInfo keysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        if (keysInfo == null) {
            throw new RestParameterException(ApiErrors.NOT_FOUND_WALLET);
        }
        EthWalletKey ethWalletKey = keysInfo.getEthWalletForAddress(ethAddress);
        if (ethWalletKey == null) {
            throw new RestParameterException(ApiErrors.NOT_FOUND_ETH_ACCOUNT);
        }

        return ethWalletKey;
    }

    @Override
    public List<String> getEthWalletAddresses(long accountId, String passphrase) {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        return walletKeysInfo.getEthWalletKeys().stream()
            .map(k -> k.getCredentials().getAddress())
            .collect(Collectors.toList());
    }
}
