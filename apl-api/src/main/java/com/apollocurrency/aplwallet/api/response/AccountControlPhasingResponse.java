/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import java.util.List;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;

public class AccountControlPhasingResponse extends ResponseBase {

    public List<AccountControlPhasingDTO> phasingOnlyControls;

}
