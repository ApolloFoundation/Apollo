/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.provider;

import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.api.dto.account.WhiteListedAccount;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WhiteListedAccountList {
    private List<WhiteListedAccount> list = new ArrayList<>();
}
