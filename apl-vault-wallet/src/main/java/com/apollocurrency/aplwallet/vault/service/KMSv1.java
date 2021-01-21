package com.apollocurrency.aplwallet.vault.service;

import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.UserKeyStore;
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
     * @param passphrase passphrase to Vault wallet.
     * @param ethWalletAddress address of the eth account.
     * @return true/false
     */
    boolean isEthKeyExist(long accountId, String passphrase, String ethWalletAddress);

    /**
     * Get information of user wallet. (apl/eth)
     * @param accountId user apl account.
     * @param passphrase passphrase to Vault wallet.
     * @return wallet information
     */
    WalletKeysInfo getWalletInfo(long accountId, String passphrase);

    /**
     * Get APL secret bytes.
     * @param accountId user apl account.
     * @param passphrase passphrase to Vault wallet.
     * @return byte[] apl secret (private key)
     */
    byte[] getAplSecretBytes(long accountId, String passphrase);

    /**
     * Store new wallet.
     * @param wallet vault wallet bytes.
     * @param passphrase passphrase to Vault wallet.
     * @return store status.
     */
    KMSResponseStatus storeWallet(byte[] wallet, String passphrase);

    /**
     * Get user key store as file.
     * @param accountId user apl account.
     * @param passphrase passphrase to Vault wallet.
     * @return key store file.
     */
    UserKeyStore exportUserKeyStore(long accountId, String passphrase);

    /**
     * Get eth user key of particular eth address.
     * @param accountId user apl account.
     * @param passphrase passphrase to Vault wallet.
     * @param ethAddress eth address.
     * @return ETH key. (Credentials)
     */
    EthWalletKey getEthWallet(long accountId, String passphrase, String ethAddress);

    /**
     * Get a list of eth address user has.
     * @param accountId user apl account.
     * @param passphrase passphrase to Vault wallet.
     * @return list of user's ETH addresses.
     */
    List<String> getEthWalletAddresses(long accountId, String passphrase);
}
