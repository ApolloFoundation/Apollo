/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DTO {
    /**
     * Time in milliseconds that took from incoming request to response
     */
    private Long requestProcessingTime = 0L;

}
