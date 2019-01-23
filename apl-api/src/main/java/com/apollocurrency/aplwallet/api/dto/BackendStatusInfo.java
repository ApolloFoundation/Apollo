/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author alukin@gmail.com
 */
@ApiModel
public class BackendStatusInfo {
    @ApiModelProperty(value = "Some parameter of backend", allowEmptyValue = true)       
    public String whatever;
}
