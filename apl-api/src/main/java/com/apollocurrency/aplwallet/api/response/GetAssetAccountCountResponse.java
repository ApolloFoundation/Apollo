package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAssetAccountCountResponse extends ResponseBase{
        public Long numberOfAccounts;
        public Long requestProcessingTime;

        public Long getNumberOfAccounts() { return numberOfAccounts; }
        public void setNumberOfAccounts(Long value) { this.numberOfAccounts = value; }

        public Long getRequestProcessingTime() { return requestProcessingTime; }
        public void setRequestProcessingTime(Long value) { this.requestProcessingTime = value; }
}
