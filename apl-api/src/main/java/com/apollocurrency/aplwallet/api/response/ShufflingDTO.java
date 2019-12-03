package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ShufflingDTO extends ResponseBase {
    private Long blocksRemaining;
    private String amount;
    private String shufflingStateHash;
    private Long requestProcessingTime;
    private String issuer;
    private String holding;
    private Long stage;
    private Long holdingType;
    private Long participantCount;
    private String assigneeRS;
    private String shuffling;
    private Long registrantCount;
    private String assignee;
    private String issuerRS;
    private String shufflingFullHash;
    private List<String> recipientPublicKeys;
}
