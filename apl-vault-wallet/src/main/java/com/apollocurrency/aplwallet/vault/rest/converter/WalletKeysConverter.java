/*
 * Copyright © 2018-2021 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.vault.rest.converter;

import com.apollocurrency.aplwallet.api.dto.WalletDTO;
import com.apollocurrency.aplwallet.api.dto.account.CurrenciesWalletsDTO;
import com.apollocurrency.aplwallet.api.dto.account.CurrencyWalletsDTO;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts {@link WalletKeysInfo} into {@link CurrenciesWalletsDTO} omitting secret data for security reasons
 */
public class WalletKeysConverter implements Converter<WalletKeysInfo, CurrenciesWalletsDTO> {
    @Override
    public CurrenciesWalletsDTO apply(WalletKeysInfo wallet) {
        CurrenciesWalletsDTO dto = new CurrenciesWalletsDTO();
        CurrencyWalletsDTO aplWallet = new CurrencyWalletsDTO();
        aplWallet.setCurrency("apl");
        aplWallet.setWallets(List.of(new WalletDTO(wallet.getAplWalletKey().getAccountRS(), Convert.toHexString(wallet.getAplWalletKey().getPublicKey()))));
        dto.addWallet(aplWallet);

        CurrencyWalletsDTO ethWallet = new CurrencyWalletsDTO();
        ethWallet.setCurrency("eth");
        ethWallet.setWallets(wallet.getEthWalletKeys()
            .stream()
            .map(e-> new WalletDTO(e.getCredentials().getAddress(), e.getCredentials().getEcKeyPair().getPublicKey().toString(16)))
            .collect(Collectors.toList()));
        dto.addWallet(ethWallet);
        return dto;
    }

}
