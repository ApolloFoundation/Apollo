/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.trading;

/**
 *
 * @author Serhiy Lymar
 */


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
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
    
    public boolean isZero() {
        return open.equals(BigDecimal.ZERO)&&close.equals(BigDecimal.ZERO)&&
                low.equals(BigDecimal.ZERO)&&high.equals(BigDecimal.ZERO)&&
                volumefrom.equals(BigDecimal.ZERO)&&volumeto.equals(BigDecimal.ZERO);
    }
}
