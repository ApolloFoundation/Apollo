/*
 * Copyright © 2018-2029 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto.utils;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DetectMimeTypeDto extends BaseDTO {
    public String type = "unknown"; // detected mime-type
}