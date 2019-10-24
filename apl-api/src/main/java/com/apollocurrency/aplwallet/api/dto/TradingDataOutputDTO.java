/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.RateLimit;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 *
 * @author Serhiy Lymar
 */
@JsonSerialize
public class TradingDataOutputDTO {
    @Schema(name="Response", description="Response description")        
    public String Response;
    @Schema(name="Type", description="Trading Data type")            
    public Integer Type;
    @Schema(name="Aggregated", description="Aggregated? ")
    public boolean Aggregated;    
    @Schema(name="Data", description="Trading data as a list of entries")
    public List<SimpleTradingEntry> Data;
    @Schema(name="TimeTo", description="TO timestamp")
    public Integer TimeTo;
    @Schema(name="TimeFrom", description="From timestamp")
    public Integer TimeFrom;
    @Schema(name="ConversionType", description="Type of conversion")
    public ConversionType ConversionType;
    @Schema(name="FirstValueInArray", description="Flag whether it is the first value")
    public boolean FirstValueInArray;
    @Schema(name="RateLimit", description="Rate Limit Structure")
    public Object RateLimit;
    @Schema(name="HasWarning", description="Flag whether the warning is available")
    public boolean HasWarning;
}
