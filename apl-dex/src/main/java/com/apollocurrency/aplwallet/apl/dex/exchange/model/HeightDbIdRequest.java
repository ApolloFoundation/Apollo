/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.dex.exchange.model;

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
public class HeightDbIdRequest {
    private long fromDbId;
    private int toHeight;
    private DexCurrency coin;
    private int limit;
}
