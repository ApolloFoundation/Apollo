package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupKeyInfoDTO extends ResponseBase {
    public Bytes group_id;
    public Bytes issuer_id;
    public Date start;
    public Date expires;
    public Boolean is_incident;
    public List<GroupKeyRecordDTO> recipients = new ArrayList<>();
    public Bytes signature = null;
}
