/*
 * Copyright © 2018-2019 Apollo Foundation
 */ 
package com.apollocurrency.aplwallet.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author Serhiy Lymar
 */

@Schema(name="DexTradeInfoMinDto", description="Minimal DEX trading information")
public class DexTradeInfoMinDto {
    @Schema(name="hi", description="the highest price for period")
    public long hi;
    @Schema(name="low", description="the lowest price for period")
    public long low;
    @Schema(name="open", description="the rate at the moment of opening")
    public long open;
    @Schema(name="close", description="the rate at the moment of closing")
    public long close;    
}

