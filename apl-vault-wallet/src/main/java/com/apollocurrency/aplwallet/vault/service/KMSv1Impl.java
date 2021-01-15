package com.apollocurrency.aplwallet.vault.service;

import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.vault.util.FbWalletUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KMSv1Impl implements KMSv1 {

    private final KeyStoreService keyStoreService;
    private final WalletKeysConverter walletKeysConverter;

    @Inject
    public KMSv1Impl(KeyStoreService keyStoreService, WalletKeysConverter walletKeysConverter) {
        this.keyStoreService = keyStoreService;
        this.walletKeysConverter = walletKeysConverter;
    }


    @Override
    public boolean isWalletExist(long accountId) {
        return keyStoreService.isSecretStoreExist(accountId);
    }

    @Override
    public boolean isEthKeyExist(long accountId, String passphrase, String ethWalletAddress) {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWallet = walletKeysInfo.getEthWalletForAddress(ethWalletAddress);
        return ethWallet != null;
    }

    @Override
    public WalletKeysInfoDTO getWalletInfo(long accountId, String passphrase) {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);

        if(walletKeysInfo != null){
            return walletKeysConverter.apply(walletKeysInfo);
        }

        return null;
    }

    @Override
    public String getAplKeySeed(long accountId, String passphrase) {
        ApolloFbWallet fbWallet = keyStoreService.getSecretStore(passphrase, accountId);
        return fbWallet != null ? fbWallet.getAplKeySecret() : null;
    }

    @Override
    public KMSResponseStatus storeWallet(byte[] wallet, String passPhrase) {
        ApolloFbWallet apolloFbWallet = FbWalletUtil.buildWallet(wallet, passPhrase);

        if (apolloFbWallet == null) {
            return KMSResponseStatus.BAD_CREDENTIALS;
        }

        return keyStoreService.saveSecretKeyStore(passPhrase, apolloFbWallet);
    }
}
