/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.trading.TradingDataOutputUpdated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.Converter;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author nemez
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
@Data
public class TradingDataOutputUpdatedDTO {
    @Schema(name="t", description="timestamps")            
    public List<Integer> t;
    @Schema(name="l", description="low values")            
    public List<BigDecimal> l;
    @Schema(name="h", description="high values")            
    public List<BigDecimal> h;
    @Schema(name="o", description="open values")            
    public List<BigDecimal> o;
    @Schema(name="c", description="close values")            
    public List<BigDecimal> c;
    @Schema(name="v", description="volumes")            
    public List<BigDecimal> v;
    @Schema(name="s", description="success")            
    public String s; 
    @Schema(name="nextTime", description="next time value")            
    public Integer nextTime;
}
