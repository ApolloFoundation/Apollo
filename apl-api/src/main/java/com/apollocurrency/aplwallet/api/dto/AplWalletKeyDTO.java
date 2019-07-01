/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter
public class AplWalletKeyDTO {
    private String account;
    private String accountRS;
    private String publicKey;
    private String passphrase;


}
