/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.trading;

/**
 *
 * @author Serhiy Lymar
 */


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SimpleTradingEntry", description = "Simple trading entry for graph data.")
public class SimpleTradingEntry {
    public Integer time;
    public BigDecimal open;
    public BigDecimal close;
    public BigDecimal low;
    public BigDecimal high;
    public BigDecimal volumefrom;
    public BigDecimal volumeto;

    @JsonIgnore
    public boolean isZero() {
        return open.equals(BigDecimal.ZERO)&&close.equals(BigDecimal.ZERO)&&
                low.equals(BigDecimal.ZERO)&&high.equals(BigDecimal.ZERO)&&
                volumefrom.equals(BigDecimal.ZERO)&&volumeto.equals(BigDecimal.ZERO);
    }

    @Override
    public boolean equals(Object o) { // do not use volumeFrom for comparison
        if (this == o) return true;
        if (!(o instanceof SimpleTradingEntry)) return false;
        SimpleTradingEntry that = (SimpleTradingEntry) o;
        return Objects.equals(time, that.time) &&
            Objects.equals(open, that.open) &&
            Objects.equals(close, that.close) &&
            Objects.equals(low, that.low) &&
            Objects.equals(high, that.high) &&
            Objects.equals(volumefrom, that.volumefrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, open, close, low, high, volumefrom);
    }
}
