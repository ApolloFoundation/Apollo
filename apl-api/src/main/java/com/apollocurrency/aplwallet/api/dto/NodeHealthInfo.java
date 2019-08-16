/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author alukin@gmail.com
 */

@Schema(name="NodeHealthInfo", description="Information about backend health")
@Getter @Setter @ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeHealthInfo {
    @Schema(name="Blockchain height", description="Current height of blockchain")
    public Integer blockchainHeight;
    public Boolean dbOK;
}
