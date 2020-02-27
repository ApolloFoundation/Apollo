/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author Serhiy Lymar
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
@Data
public class TradingDataOutputDTO extends ResponseBase {
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
