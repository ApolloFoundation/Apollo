package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollDTO extends BaseDTO {
    private Long minRangeValue;
    private Long votingModel;
    private String description;
    private Boolean finished;
    private String poll;
    private Long requestProcessingTime;
    private Long minNumberOfOptions;
    private String minBalance;
    private String accountRS;
    private String name;
    private String[] options;
    private Long finishHeight;
    private Long maxNumberOfOptions;
    private Long minBalanceModel;
    private String account;
    private Long maxRangeValue;
    private Long timestamp;
}

