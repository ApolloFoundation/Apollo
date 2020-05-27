/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */
package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountBlocksResponse extends ResponseBase {
    private List<BlockDTO> blocks;
}
