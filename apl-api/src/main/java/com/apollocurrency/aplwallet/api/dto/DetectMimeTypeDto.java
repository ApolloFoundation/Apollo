/*
 * Copyright © 2018-2029 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DetectMimeTypeDto extends BaseDTO {
    public String type = "unknown"; // detected mime-type
}