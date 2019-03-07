package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.firstbridge.fbc.core.account.LedgerEvent;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountLedgerResponse {
    public EntryDTO[] entries;
    public long requestProcessingTime;
    public LedgerEvent eventType;

}
