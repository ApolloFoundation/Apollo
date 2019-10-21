package com.apollocurrency.aplwallet.api.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class VaultWalletResponse extends ResponseBase {
    private String fileName;
    private String file;
}
