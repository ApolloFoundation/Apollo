/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.dto.AplWalletDTO;
import com.apollocurrency.aplwallet.api.dto.AplWalletKeyDTO;
import com.apollocurrency.aplwallet.api.dto.EthWalletKeyDTO;
import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

@Singleton
public class WalletKeysConverter implements Converter<WalletKeysInfo, WalletKeysInfoDTO> {

    @Override
    public WalletKeysInfoDTO apply(WalletKeysInfo wallet) {
        WalletKeysInfoDTO dto = new WalletKeysInfoDTO();
        dto.setAccount(Long.toUnsignedString(wallet.getAplWalletKey().getId()));
        dto.setAccountRS(Convert2.rsAccount(wallet.getAplWalletKey().getId()));
        dto.setPublicKey(Convert.toHexString(wallet.getAplWalletKey().getPublicKey()));
        dto.setPassphrase(wallet.getPassphrase());

        AplWalletKeyDTO aplWalletKeyDTO = new AplWalletKeyDTO(
            dto.getAccount(), dto.getAccountRS(),
            dto.getPublicKey(), dto.getPassphrase());

        dto.setApl(new AplWalletDTO(aplWalletKeyDTO));
        wallet.getEthWalletKeys().forEach(ethWalletKey -> dto.addEthWalletKey(
            new EthWalletKeyDTO(ethWalletKey.getCredentials().getAddress(),
                ethWalletKey.getCredentials().getEcKeyPair().getPublicKey().toString(16))));
        return dto;
    }

}
