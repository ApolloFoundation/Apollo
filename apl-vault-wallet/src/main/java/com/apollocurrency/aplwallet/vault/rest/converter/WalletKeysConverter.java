/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.vault.rest.converter;

import com.apollocurrency.aplwallet.api.dto.AplWalletDTO;
import com.apollocurrency.aplwallet.api.dto.AplWalletKeyDTO;
import com.apollocurrency.aplwallet.api.dto.EthWalletKeyDTO;
import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;

/**
 * Converts {@link WalletKeysInfo} into {@link WalletKeysInfoDTO} omitting secret data for security reasons
 */
public class WalletKeysConverter implements Converter<WalletKeysInfo, WalletKeysInfoDTO> {
    @Override
    public WalletKeysInfoDTO apply(WalletKeysInfo wallet) {
        WalletKeysInfoDTO dto = new WalletKeysInfoDTO();
        dto.setAccount(Long.toUnsignedString(wallet.getAplWalletKey().getId()));
        dto.setAccountRS(Convert2.rsAccount(wallet.getAplWalletKey().getId()));
        dto.setPublicKey(Convert.toHexString(wallet.getAplWalletKey().getPublicKey()));
        dto.setPassphrase("*****");
        AplWalletKeyDTO aplWalletKeyDTO = new AplWalletKeyDTO(
            dto.getAccount(), dto.getAccountRS(),
            dto.getPublicKey(), "*****");

        dto.setApl(new AplWalletDTO(aplWalletKeyDTO));
        wallet.getEthWalletKeys().forEach(ethWalletKey -> dto.addEthWalletKey(
            new EthWalletKeyDTO(ethWalletKey.getCredentials().getAddress(),
                ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(16))));
        return dto;
    }

}
