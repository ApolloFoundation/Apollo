package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DGSGoodsDTO extends BaseDTO {
    private String seller;
    private Long quantity;
    private String goods;
    private String description;
    private Long priceATM;
    private String sellerRS;
    private Boolean hasImage;
    private Long requestProcessingTime;
    private Boolean delisted;
    private List<String> parsedTags;
    private String tags;
    private String name;
    private Long timestamp;
}
