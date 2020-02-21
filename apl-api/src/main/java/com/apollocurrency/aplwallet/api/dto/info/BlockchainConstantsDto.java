/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

//@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class BlockchainConstantsDto extends BaseDTO {
    public String genesisBlockId;
    public String genesisAccountId;
    public long epochBeginning;
    public int maxArbitraryMessageLength;
    public int maxPrunableMessageLength;
    public String coinSymbol;
    public String accountPrefix;
    public String projectName;
    public int maxImportSecretFileLength;
    public BigInteger gasLimitEth;
    public BigInteger gasLimitERC20;
    public List<SubTypeDto> transactionSubTypes = new ArrayList<>();
    public List<List<SubTypeDto>> transactionTypes = new ArrayList<>();
}
