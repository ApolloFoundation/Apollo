/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Serhiy Lymar
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DexOrderDBRequestForTrading {
    private Integer startInterval;
    private Integer endInterval;
    private byte pairCur;
    private byte requestedType;
    private Integer offset;
    private Integer limit;
}
