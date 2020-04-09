/*
 * Copyright © 2018 - 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.trading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Serhii Lymar
 */

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "TradingDataOutput", description = "Trading data output")
public class TradingDataOutput {
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

    public void init() {
        t = new ArrayList<>();
        l = new ArrayList<>();
        h = new ArrayList<>();
        o = new ArrayList<>();
        c = new ArrayList<>();
        v = new ArrayList<>();
    }
}
