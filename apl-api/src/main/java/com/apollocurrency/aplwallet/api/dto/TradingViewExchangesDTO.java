/*
 * Copyright © 2018 - 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author Serhiy Lymar
 */

@JsonSerialize
@Data
public class TradingViewExchangesDTO {
        @Schema(name="value", description="TV exchange value")            
        public String value;
        @Schema(name="name", description="TV exchange name")            
        public String name;
        @Schema(name="desc", description="TV exchange description")            
        public String desc;     
}
