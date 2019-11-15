/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.trading;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
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
// @AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
// @Schema(name = "CacheStatsResponse", description = "The cache stats response.")
public class TradingDataOutput {
    String Response;
    Integer Type;
    boolean Aggregated;
    List<SimpleTradingEntry> Data;
    Integer TimeTo;
    Integer TimeFrom;
    boolean FirstValueInArray;
    Object RateLimit;
    ConversionType ConversionType;
    
    boolean HasWarning;
    
}
