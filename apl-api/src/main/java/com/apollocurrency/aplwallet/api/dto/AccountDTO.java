/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Schema(name = "AccountDTO", description = "Information about account, asset, lease, balance etc")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
public class AccountDTO extends BaseDTO {
    private String account;
    private String accountRS;
    private String publicKey;
    private String passphrase;
    private String secret;
    //account info
    @JsonProperty("is2FA")
    private boolean is2FA;
    private String name;
    private String description;
    //account controls
    private Set<String> accountControls;
    //balance
    @JsonSerialize(using = ToStringSerializer.class)
    private Long balanceATM;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long forgedBalanceATM;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unconfirmedBalanceATM;
    //effective balance
    private Long effectiveBalanceAPL;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long guaranteedBalanceATM;
    //account Lease
    private String currentLessee;
    private Integer currentLeasingHeightFrom;
    private Integer currentLeasingHeightTo;
    private String nextLessee;
    private Integer nextLeasingHeightFrom;
    private Integer nextLeasingHeightTo;
    //account lessors
    private List<String> lessors;
    private List<String> lessorsRS;
    private List<AccountLeaseDTO> lessorsInfo;
    //account assets
    public List<AccountAssetBalanceDTO> assetBalances;
    public List<AccountAssetUnconfirmedBalanceDTO> unconfirmedAssetBalances;
    //account currency
    public List<AccountCurrencyDTO> accountCurrencies;

    public AccountDTO(long account, String accountRS, long balanceATM, long forgedBalanceATM, long unconfirmedBalanceATM) {
        this.account = Long.toUnsignedString(account);
        this.accountRS = accountRS;
        this.balanceATM = balanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
    }

    @JsonIgnore
    public long getId() {
        return Long.parseLong(account);
    }

}
