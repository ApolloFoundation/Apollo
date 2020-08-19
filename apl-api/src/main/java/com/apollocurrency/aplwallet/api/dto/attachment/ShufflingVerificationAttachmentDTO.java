/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

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
public class ShufflingVerificationAttachmentDTO extends AbstractShufflingAttachmentDTO {
}
