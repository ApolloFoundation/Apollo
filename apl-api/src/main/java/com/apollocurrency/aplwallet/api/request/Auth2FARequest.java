/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.request;

import lombok.Data;

@Data
public class Auth2FARequest {
    private String account;
    private String passphrase;
    private int code2FA;
}
