package com.apollocurrency.aplwallet.api.response;


import com.apollocurrency.aplwallet.api.dto.Property;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountPropertiesResponse extends ResponseBase {
    public String recipientRS;
    public String recipient;
    public long requestProcessingTime;
    public List<Property> properties;
}
