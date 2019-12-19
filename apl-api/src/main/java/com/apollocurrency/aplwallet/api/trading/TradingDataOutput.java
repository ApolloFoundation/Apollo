/*
 * Copyright © 2018-2019 Apollo Foundation
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

//   Response":"Success",
//   "Type":100,
//   "Aggregated":false,
//   "Data":[ 
//      { 
//         "time":1571553660,
//         "close":7925,
// ....

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "TradingDataOutput", description = "Trading data output")
public class TradingDataOutput {
    @JsonProperty("Response")
    String response;
    @JsonProperty("Type")
    Integer type;    
    @JsonProperty("Aggregated")
    boolean aggregated;
    @JsonProperty("Data")
    List<SimpleTradingEntry> data;
    @JsonProperty("TimeTo")
    Integer timeTo;
    @JsonProperty("TimeFrom")
    Integer timeFrom;
    @JsonProperty("FirstValueInArray")
    boolean firstValueInArray;
    @JsonProperty("RateLimit")
    Object rateLimit;
    @JsonProperty("ConversionType")
    ConversionType conversionType;
    @JsonProperty("HasWarning")
    boolean hasWarning;    
}
