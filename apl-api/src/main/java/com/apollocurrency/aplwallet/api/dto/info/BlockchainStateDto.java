/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class BlockchainStateDto extends BlockchainStatusDto {

    public BlockchainStateDto(BlockchainStatusDto stateDto) {
        super(stateDto);
    }
}
