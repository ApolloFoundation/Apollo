/*
 *  Copyright © 2019-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Block global secondary index entity.
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockIndex {
    private long blockId;
    private int blockHeight;

    public BlockIndex copy() {
        return new BlockIndex(blockId, blockHeight);
    }
}
