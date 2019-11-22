package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

public class TaggedDataDTO extends BaseDTO {
    private String data;
    private String channel;
    private String description;
    private String type;
    private List<String> parsedTags;
    private Long transactionTimestamp;
    private String tags;
    private String filename;
    private String accountRS;
    private String name;
    private Long blockTimestamp;
    private String transaction;
    private String account;
    private Boolean isText;
}
