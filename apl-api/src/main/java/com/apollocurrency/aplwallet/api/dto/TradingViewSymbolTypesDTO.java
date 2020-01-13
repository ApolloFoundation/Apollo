/*
 * Copyright © 2018-2020 Apollo Foundation
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
public class TradingViewSymbolTypesDTO {
        @Schema(name="value", description="TV view symbols value")            
        public String value;
        @Schema(name="name", description="TV view symbols name")            
        public String name;
}
