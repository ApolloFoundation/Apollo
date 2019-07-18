/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexTradeInfoMinDto;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Serhiy Lymar
 */

@Getter @Setter
public class DexTradeEntryMin {    
     private long hi;
     private long low;
     private long open;
     private long close;

public DexTradeInfoMinDto toDto(){       
        DexTradeInfoMinDto dexTradeInfoMinDto = new DexTradeInfoMinDto();        
        dexTradeInfoMinDto.hi = this.hi;
        dexTradeInfoMinDto.low = this.low;
        dexTradeInfoMinDto.open = this.open;
        dexTradeInfoMinDto.close = this.close;        
        return dexTradeInfoMinDto;
    }
}
