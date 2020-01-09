/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author nemez
 */
@JsonSerialize
@Data
public class TradingViewSymbolTypesDTO {
        @Schema(name="value", description="TV view symbols value")            
        public String value;
        @Schema(name="name", description="TV view symbols name")            
        public String name;
}
