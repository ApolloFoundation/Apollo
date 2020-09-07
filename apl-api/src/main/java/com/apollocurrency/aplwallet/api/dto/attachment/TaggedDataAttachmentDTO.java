/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
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
public class TaggedDataAttachmentDTO {
    public String name;
    public String description;
    public String tags;
    public String type;
    public String channel;
    public boolean isText;
    public String filename;
    public byte[] data;
    public TaggedDataDTO taggedData;

}
