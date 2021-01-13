/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProcessBlockRequest extends BaseP2PRequest {
    public BlockDTO block;
    public String previousBlock;
    public int timestamp;
    public int timeout;

    ProcessBlockRequest(UUID chainId) {
        super("processBlock", chainId);
    }

    public ProcessBlockRequest(BlockDTO block, UUID chainId) {
        this(chainId);
        this.block = block;
        this.timestamp = block.getTimestamp();
        this.timeout = block.getTimeout();
        this.previousBlock = block.getPreviousBlock();
    }
}
