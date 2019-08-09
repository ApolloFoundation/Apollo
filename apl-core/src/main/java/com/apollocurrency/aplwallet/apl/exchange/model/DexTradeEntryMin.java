/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Serhiy Lymar
 */

@Getter @Setter
public class DexTradeEntryMin {    
     private BigDecimal hi;
     private BigDecimal low;
     private BigDecimal open;
     private BigDecimal close;
}
