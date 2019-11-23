/*
 * Copyright © 2018-2019 Apollo Foundation
 */ 
package com.apollocurrency.aplwallet.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 *
 * @author Serhiy Lymar
 */

@Schema(name="DexTradeInfoMinDto", description="Minimal DEX trading information")
public class DexTradeInfoMinDto {
    @Schema(name="hi", description="the highest price for period")
    public BigDecimal hi;
    @Schema(name="low", description="the lowest price for period")
    public BigDecimal low;
    @Schema(name="open", description="the rate at the moment of opening")
    public BigDecimal open;
    @Schema(name="close", description="the rate at the moment of closing")
    public BigDecimal close;    
    @Schema(name="volumefrom", description="min volume of trade operations")
    public BigDecimal volumefrom;
    @Schema(name="volumeto", description="max volume of trade operations")
    public BigDecimal volumeto;        
}

