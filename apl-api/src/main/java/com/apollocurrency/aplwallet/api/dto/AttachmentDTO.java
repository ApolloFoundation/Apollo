package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentDTO {
    public Long versionOrdinaryPayment;
    public Byte version;
    public Long versionCriticalUpdate;
    public String platform;
    public String hash;
    public String architecture;
    public Long versionSetPhasingOnly;
    public String controlMaxFees;
    public Long controlMinDuration;
    public Long controlMaxDuration;
    public Long versionAssetTransfer;
    public String quantityQNT;
    public String asset;
    public String name;
    public String description;
    public Long versionAssetIssuance;
    public Long decimals;
    public String priceNQT;
    public Long versionBidOrderPlacement;
    public Long versionAskOrderPlacement;
    public Long versionAccountInfo;
    public String filename;
    public String data;
    public String channel;
    public String type;
    public Long versionTaggedDataUpload;
    public Boolean isText;
    public String tags;
}
