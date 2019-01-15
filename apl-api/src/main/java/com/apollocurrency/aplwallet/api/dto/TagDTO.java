package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDTO {
    public String tag;
    public Integer height;
    public Integer count;
}
