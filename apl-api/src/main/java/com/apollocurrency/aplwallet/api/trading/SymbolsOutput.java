/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.trading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Serhiy Lymar
 */


/*
name: "AAPL"
exchange-traded: "NasdaqNM"
exchange-listed: "NasdaqNM"
timezone: "America/New_York"
minmov: 1
minmov2: 0
pointvalue: 1
session: "0930-1630"
has_intraday: false
has_no_volume: false
description: "Apple Inc."
type: "stock"
supported_resolutions: ["D", "2D", "3D", "W", "3W", "M", "6M"]
pricescale: 100
ticker: "AAPL"
*/
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SymbolsOutput", description = "Trading data output")
public class SymbolsOutput {
    @JsonProperty("name")
    String name;
    @JsonProperty("exchange-traded")
    String exchange_traded;
    @JsonProperty("exchange-listed")
    String exchange_listed;
    @JsonProperty("timezone")
    String timezone;
    @JsonProperty("minmov")
    Integer minmov;
    @JsonProperty("minmov")
    Integer minmov2;
    @JsonProperty("pointvalue")
    Integer pointvalue;    
    @JsonProperty("session")
    String session;
    @JsonProperty("has_intraday")
    boolean intraday;
    @JsonProperty("has_no_volume")
    boolean no_volume;
    @JsonProperty("description")
    String description;
    @JsonProperty("type")
    String type;
    @JsonProperty("supported_resolutions")
    List<String> supported_resolutions;
    @JsonProperty("pricescale")
    Integer pricescale;  
    @JsonProperty("ticker")
    String ticker;    
}
