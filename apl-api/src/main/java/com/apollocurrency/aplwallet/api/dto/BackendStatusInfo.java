/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author alukin@gmail.com
 */
@Schema(name="BackStatusInfo", description="Information about backend state")
public class BackendStatusInfo {
    @Schema(name="Some param", description="Description for param")
    public String whatever;
}
