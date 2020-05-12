/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;

public class ShardToDtoConverter implements Converter<Shard, ShardDTO> {

    @Override
    public ShardDTO apply(Shard shard) {
        ShardDTO dto = new ShardDTO();
        dto.shardId = shard.getShardId();
        dto.shardHash = Convert.toHexString(shard.getShardHash());
        dto.shardState = shard.getShardState() != null? shard.getShardState().getValue() : ShardState.INIT.getValue();
        dto.shardHeight = shard.getShardHeight();
        dto.coreZipHash = Convert.toHexString(shard.getCoreZipHash());
        dto.prunableZipHash = Convert.toHexString(shard.getPrunableZipHash());
        dto.generatorIds = Arrays.toString(shard.getGeneratorIds());
        dto.blockTimeouts = Arrays.toString(shard.getBlockTimeouts());
        dto.blockTimestamps = Arrays.toString(shard.getBlockTimestamps());
        return dto;
    }
}
