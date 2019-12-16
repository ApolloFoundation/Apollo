package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class BlockListInfoResponse extends ResponseBase {
    private List<BlockDTO> blocks = new ArrayList<>(0);

    @Override
    public String toString() {
        return "BlockListInfoResponse{" +
                "blocks=[" + (blocks != null ? blocks.size() : 0) +
                "]}";
    }
}
