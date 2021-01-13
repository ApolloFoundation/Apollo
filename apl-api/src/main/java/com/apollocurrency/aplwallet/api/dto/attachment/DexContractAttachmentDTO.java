/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

import com.apollocurrency.aplwallet.api.dto.AppendixDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DexContractAttachmentDTO extends AppendixDTO {
    public String orderId;
    public String counterOrderId;
    public String secretHash;
    public String transferTxId;
    public String counterTransferTxId;
    public String encryptedSecret;
    public byte contractStatus;
    public Integer timeToReply;
}
