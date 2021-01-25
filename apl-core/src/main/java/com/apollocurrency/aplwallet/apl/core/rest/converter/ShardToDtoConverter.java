/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import java.util.Arrays;

@Singleton
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
