/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.api.trading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author Serhii Lymar
 */

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "TradingDataOutput", description = "Trading data output")
public class TradingDataOutputUpdated {
    @JsonProperty("t")
    List<Integer> t;
    @JsonProperty("l")
    List<BigDecimal> l;
    @JsonProperty("h")
    List<BigDecimal> h;
    @JsonProperty("o")
    List<BigDecimal> o;
    @JsonProperty("c")
    List<BigDecimal> c;
    @JsonProperty("v")
    List<BigDecimal> v;
    @JsonProperty("s")
    String s; 
    @JsonProperty("nextTime")
    Integer nextTime;
}
