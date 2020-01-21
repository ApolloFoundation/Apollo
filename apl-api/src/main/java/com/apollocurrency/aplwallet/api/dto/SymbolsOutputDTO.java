/*
 * Copyright © 2018 - 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 *
 * @author Serhiy Lymar
 */
@JsonSerialize
@Data
public class SymbolsOutputDTO {
    @Schema(name="name", description="symbol name")            
    public String name;   
    @Schema(name="exchange-traded", description="exchange traded")
    public String exchange_traded;    
    @Schema(name="exchange-listed", description="exchange listed")
    public String exchange_listed;
    @Schema(name="timezone", description="time zone")
    public String timezone;    
    @Schema(name="minmov", description="min mov")
    public Integer minmov;
    @Schema(name="minmov2", description="min mov 2")
    public Integer minmov2;
    @Schema(name="pointvalue", description="point value")
    public Integer pointvalue;    
    @Schema(name="session", description="session value")    
    public String session;        
    @Schema(name="has_intraday", description="has intraday value")    
    public boolean has_intraday;    
    @Schema(name="has_no_volume", description="has no volume value") 
    public boolean has_no_volume;    
    @Schema(name="description", description="output description value") 
    public String description;    
    @Schema(name="type", description="output type value")     
    public String type;    
    @Schema(name="supported_resolutions", description="supported resolutions list")     
    public List<String> supported_resolutions;
    @Schema(name="pricescale", description="price scale")     
    public Integer pricescale;  
    @Schema(name="ticker", description="ticker value")     
    public String ticker;  
    @Schema(name="has_empty_bars", description="has empty bars") 
    public boolean has_empty_bars;
    @Schema(name="has_weekly_and_monthly", description="has weekly and monthly") 
    public boolean has_weekly_and_monthly;
    @Schema(name="has_daily", description="has daily") 
    public boolean has_daily;
}
