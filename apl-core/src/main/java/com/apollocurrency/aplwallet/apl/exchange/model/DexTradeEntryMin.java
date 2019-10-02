/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 *
 * @author Serhiy Lymar
 */

@Data
public class DexTradeEntryMin {    
     private BigDecimal hi;
     private BigDecimal low;
     private BigDecimal open;
     private BigDecimal close;
}
