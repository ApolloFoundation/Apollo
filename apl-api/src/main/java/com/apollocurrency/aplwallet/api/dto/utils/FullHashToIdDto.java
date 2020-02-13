/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto.utils;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FullHashToIdDto extends BaseDTO {
    public String longId; // long from hash
    public String stringId; // unsigned long from hash
}