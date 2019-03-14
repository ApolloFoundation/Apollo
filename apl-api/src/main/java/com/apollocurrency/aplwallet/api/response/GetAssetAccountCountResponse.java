package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAssetAccountCountResponse extends ResponseBase{
        public Long numberOfAssets;
        public Long requestProcessingTime;

        public Long getNumberOfAccounts() { return numberOfAssets; }
        public void setNumberOfAccounts(Long value) { this.numberOfAssets = value; }

        public Long getRequestProcessingTime() { return requestProcessingTime; }
        public void setRequestProcessingTime(Long value) { this.requestProcessingTime = value; }
}
