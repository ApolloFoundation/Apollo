package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaggedDataDTO {
    public Long id;
    public String transaction;
    public Long accountId;
    public String name;
    public String description;
    public String tags;
    public String[] parsedTags;
    public String data;
    public String type;
    public String channel;
    public Boolean isText;
    public String filename;
    public Integer transactionTimestamp;
    public Integer blockTimestamp;
    public Integer height;
    public String account;
    public String accountRS;
}
