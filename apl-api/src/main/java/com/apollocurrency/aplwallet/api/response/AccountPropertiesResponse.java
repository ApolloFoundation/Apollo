package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AccountPropertyDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountPropertiesResponse extends ResponseBase {
    private String recipientRS;
    private String recipient;
    private long requestProcessingTime;
    private List<AccountPropertyDTO> properties;
}
