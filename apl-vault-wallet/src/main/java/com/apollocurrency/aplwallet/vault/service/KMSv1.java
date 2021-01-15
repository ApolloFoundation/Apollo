package com.apollocurrency.aplwallet.vault.service;

import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;

public interface KMSv1 {
    /**
     * Is Vault wallet exist for this account.
     * @param accountId
     * @return true/false
     */
    boolean isWalletExist(long accountId);

    boolean isEthKeyExist(long accountId, String passphrase, String ethWalletAddress);

    WalletKeysInfoDTO getWalletInfo(long accountId, String passphrase);

    String getAplKeySeed(long accountId, String passphrase);

    KMSResponseStatus storeWallet(byte[] wallet, String passphrase);




}
