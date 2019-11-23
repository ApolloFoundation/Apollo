/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class WalletKeysInfoDTO extends BaseDTO {
    private String account;
    private String accountRS;
    private String publicKey;
    private String passphrase;
    private AplWalletDTO apl;
    private List<EthWalletKeyDTO> eth = new ArrayList<>();

    public void addEthWalletKey(EthWalletKeyDTO dto) {
        eth.add(dto);
    }

}
