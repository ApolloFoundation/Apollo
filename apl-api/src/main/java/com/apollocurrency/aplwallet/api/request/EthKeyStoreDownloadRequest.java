package com.apollocurrency.aplwallet.api.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EthKeyStoreDownloadRequest extends Auth2FARequest {
    private String ethAddress;
    private String ethKeystorePassword;
}
