package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Block global secondary index entity.
 */
@Getter @Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class BlockIndex {
    private long blockId;
    private int blockHeight;

    public BlockIndex copy() {
        return new BlockIndex(blockId, blockHeight);
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private long blockId;
        private int blockHeight;

        private ShardBuilder() {
        }

        public ShardBuilder blockId(long blockId) {
            this.blockId = blockId;
            return this;
        }

        public ShardBuilder blockHeight(int blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        public BlockIndex build() {
            return new BlockIndex(blockId, blockHeight);
        }
    }
}
