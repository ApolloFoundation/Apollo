/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

import com.apollocurrency.aplwallet.api.dto.AppendixDTO;
import com.apollocurrency.aplwallet.api.dto.DoubleByteArrayTupleDTO;
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
public class UpdateAttachmentDTO extends AppendixDTO {
    public String os;
    public String platform;
    public String architecture;
    public DoubleByteArrayTupleDTO url;
    public String updateVersion;
    public String hash;

}
