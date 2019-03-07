package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;


//@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaggedDataResponse extends ResponseBase {
    // @ApiModelProperty
    public Long id;
    //@ApiModelProperty(value = "Transaction Id")
    public String transaction;
    // @ApiModelProperty
    public Long accountId;
    //@ApiModelProperty
    public String name;
    //@ApiModelProperty
    public String description;
    //@ApiModelProperty
    public String tags;
    // @ApiModelProperty
    public String[] parsedTags;
    //@ApiModelProperty
    public String data;
    //@ApiModelProperty
    public String type;
    //@ApiModelProperty
    public String channel;
    //@ApiModelProperty
    public Boolean isText;
    //@ApiModelProperty
    public String filename;
    //@ApiModelProperty
    public Integer transactionTimestamp;
    //@ApiModelProperty
    public Integer blockTimestamp;
    //@ApiModelProperty
    public Integer height;
    //@ApiModelProperty
    public String account;
    //@ApiModelProperty
    public String accountRS;

}
