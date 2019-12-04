/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Serhiy Lymar
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeightDbIdRequest {
    private long fromDbId;
    private int fromHeight;
    private int toHeight;
    private byte coin;
    private int limit;
}
