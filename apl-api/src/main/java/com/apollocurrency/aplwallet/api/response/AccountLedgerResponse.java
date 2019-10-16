package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountLedgerResponse extends ResponseBase {
    private List<EntryDTO> entries;
    private Long requestProcessingTime;
}
