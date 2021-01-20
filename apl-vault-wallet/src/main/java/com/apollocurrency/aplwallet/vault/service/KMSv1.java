package com.apollocurrency.aplwallet.vault.service;

import com.apollocurrency.aplwallet.api.dto.vault.ExportKeyStore;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;

import java.util.List;

public interface KMSv1 {
    /**
     * Is Vault wallet exist for this account.
     * @param accountId user apl account.
     * @return true/false
     */
    boolean isWalletExist(long accountId);

    /**
     * Is Eth wallet exist for this eth address.
     * @param accountId user apl account.
     * @param passphrase passphrase from Vault wallet.
     * @param ethWalletAddress address of the eth account.
     * @return true/false
     */
    boolean isEthKeyExist(long accountId, String passphrase, String ethWalletAddress);

    WalletKeysInfo getWalletInfo(long accountId, String passphrase);

    byte[] getAplSecretBytes(long accountId, String passphrase);

    KMSResponseStatus storeWallet(byte[] wallet, String passphrase);

    ExportKeyStore exportKeyStore(long accountId, String passphrase);

    EthWalletKey getEthWallet(long accountId, String passphrase, String ethAddress);

    List<String> getEthWalletAddresses(long accountId, String passphrase);
}
