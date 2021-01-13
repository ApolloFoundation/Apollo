/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import java.util.List;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountControlPhasingResponse extends ResponseBase {

    public List<AccountControlPhasingDTO> phasingOnlyControls;

}
