package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetPeersIpResponse extends ResponseBase {
    private List<String> peers;
}
